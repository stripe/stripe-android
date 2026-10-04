package com.stripe.android.payments

import android.app.Activity
import android.content.ActivityNotFoundException
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePaddingRelative
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.core.exception.StripeException
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.view.PaymentAuthWebViewActivity

/**
 * A transparent activity that launches [PaymentBrowserAuthContract.Args.url] in
 * Custom Tabs (if available), a browser, or the native UPI app chooser.
 *
 * The eventual replacement for [PaymentAuthWebViewActivity].
 *
 * [PaymentBrowserAuthContract] selects this activity for the app chooser, the SDK default
 * return URL, or instant apps. Returning from an external app triggers intent verification.
 */
internal class StripeBrowserLauncherActivity : AppCompatActivity() {
    private val args: PaymentBrowserAuthContract.Args? by lazy {
        PaymentBrowserAuthContract.parseArgs(intent)
    }

    private val viewModel: StripeBrowserLauncherViewModel by viewModels {
        StripeBrowserLauncherViewModel.Factory(requireNotNull(args))
    }

    private var hasLeftForAppChooser = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = args
        if (args == null) {
            finish()
            ErrorReporter.createFallbackInstance(
                applicationContext,
                apiConfigurationProvider = { error("StripeBrowserLauncherActivity was started without arguments.") },
            )
                .report(
                    errorEvent = ErrorReporter.ExpectedErrorEvent.BROWSER_LAUNCHER_NULL_ARGS,
                )
            return
        }

        if (viewModel.hasLaunched) {
            finishWithSuccess(args)
        } else {
            launchBrowser(args)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePaddingRelative(systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onStop() {
        if (args?.shouldUseAppChooser == true && viewModel.hasLaunched && !isFinishing) {
            hasLeftForAppChooser = true
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        // Some UPI apps do not deliver an activity result when the customer returns manually.
        // Returning to checkout is a signal to verify the intent, not proof of payment success.
        if (hasLeftForAppChooser && !isFinishing) {
            args?.let(::finishWithSuccess)
        }
    }

    private fun launchBrowser(args: PaymentBrowserAuthContract.Args) {
        val contract = ActivityResultContracts.StartActivityForResult()
        val launcher = registerForActivityResult(contract) {
            finishWithSuccess(args)
        }

        try {
            val intent = viewModel.createLaunchIntent(args)
            launcher.launch(intent)
            viewModel.hasLaunched = true
        } catch (e: ActivityNotFoundException) {
            finishWithFailure(args, e)
        } catch (e: SecurityException) {
            finishWithFailure(args, e)
        } catch (e: IllegalArgumentException) {
            finishWithFailure(args, e)
        }
    }

    private fun finishWithSuccess(args: PaymentBrowserAuthContract.Args) {
        setResult(
            Activity.RESULT_OK,
            viewModel.getResultIntent(args)
        )
        finish()
    }

    private fun finishWithFailure(args: PaymentBrowserAuthContract.Args, cause: Exception) {
        ErrorReporter.createFallbackInstance(
            applicationContext,
            apiConfigurationProvider = { args.apiConfiguration },
        ).report(
            errorEvent = ErrorReporter.ExpectedErrorEvent.BROWSER_LAUNCHER_ACTIVITY_NOT_FOUND,
            stripeException = StripeException.create(cause),
        )
        setResult(
            Activity.RESULT_OK,
            viewModel.getFailureIntent(args, cause)
        )
        finish()
    }
}
