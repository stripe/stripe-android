package com.stripe.android.payments

import android.app.Activity
import android.content.ActivityNotFoundException
import android.os.Bundle
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
        StripeBrowserLauncherViewModel.Factory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = PaymentBrowserAuthContract.parseArgs(intent)
        if (args == null) {
            // handle failures
            finish()
            return
        }

        if (viewModel.hasLaunched) {
            finishWithSuccess(args)
        } else {
            launchBrowser(args)
        }
    }

    private fun launchBrowser(args: PaymentBrowserAuthContract.Args) {
        val contract = ActivityResultContracts.StartActivityForResult()
        val launcher = registerForActivityResult(contract) {
            finishWithSuccess(args)
        }

        val intent = viewModel.createLaunchIntent(args)

        try {
            launcher.launch(intent)
            viewModel.hasLaunched = true
        } catch (e: ActivityNotFoundException) {
            finishWithFailure(args)
        }
    }

    private fun finishWithSuccess(args: PaymentBrowserAuthContract.Args) {
        setResult(
            Activity.RESULT_OK,
            viewModel.getResultIntent(args)
        )
        finish()
    }

    private fun finishWithFailure(args: PaymentBrowserAuthContract.Args) {
        setResult(
            Activity.RESULT_OK,
            viewModel.getFailureIntent(args)
        )
        finish()
    }
}
