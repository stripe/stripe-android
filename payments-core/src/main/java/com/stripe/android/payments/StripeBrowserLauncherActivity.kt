package com.stripe.android.payments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.view.PaymentAuthWebViewActivity

/**
 * A transparent activity that launches [PaymentBrowserAuthContract.Args.url] in either
 * Custom Tabs (if available) or a browser.
 *
 * The eventual replacement for [PaymentAuthWebViewActivity].
 *
 * [StripeBrowserLauncherActivity] will only be used when the following are true:
 * - Custom Tabs are available or Chrome is installed
 * - the confirmation request's `return_url` is set to [DefaultReturnUrl.value]
 *
 * See [BrowserCapabilities] and [PaymentBrowserAuthContract.Args.hasDefaultReturnUrl].
 */
internal class StripeBrowserLauncherActivity : AppCompatActivity() {
    private val viewModel: StripeBrowserLauncherViewModel by viewModels {
        StripeBrowserLauncherViewModel.Factory(
            application,
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = PaymentBrowserAuthContract.parseArgs(intent)
        if (args == null) {
            // handle failures
            finish()
            return
        }

        setResult(
            Activity.RESULT_OK,
            viewModel.getResultIntent(args)
        )

        if (viewModel.hasLaunched) {
            finish()
        } else {
            val launcher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
                ::onResult
            )

            launcher.launch(
                viewModel.createLaunchIntent(args)
            )

            viewModel.hasLaunched = true
        }
    }

    private fun onResult(activityResult: ActivityResult) {
        // always dismiss the activity when a result is available

        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // This is invoked by the intent filter. We might not need to implement this method but
        // leaving it here for posterity.
    }
}
