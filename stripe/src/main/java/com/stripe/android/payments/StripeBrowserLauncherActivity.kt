package com.stripe.android.payments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.stripe.android.auth.PaymentAuthWebViewContract
import com.stripe.android.view.PaymentAuthWebViewActivity

/**
 * A transparent activity that launches [PaymentAuthWebViewContract.Args.url] in either
 * Custom Tabs (if available) or a browser.
 *
 * The eventual replacement for [PaymentAuthWebViewActivity].
 */
internal class StripeBrowserLauncherActivity : AppCompatActivity() {
    private val viewModel: StripeBrowserLauncherViewModel by viewModels {
        StripeBrowserLauncherViewModel.Factory(application)
    }

    private val customTabsCapabilities: CustomTabsCapabilities by lazy {
        CustomTabsCapabilities(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = PaymentAuthWebViewContract.parseArgs(intent)
        if (args == null) {
            // handle failures
            finish()
            return
        }

        val url = Uri.parse(args.url)

        setResult(
            Activity.RESULT_OK,
            viewModel.getResultIntent(args)
        )

        val launcher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::onResult
        )

        val shouldUseCustomTabs = customTabsCapabilities.isSupported()
        viewModel.logAnalytics(shouldUseCustomTabs)

        if (shouldUseCustomTabs) {
            // use Custom Tabs
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .build()
            customTabsIntent.intent.data = url
            launcher.launch(customTabsIntent.intent)
        } else {
            // use default device browser
            launcher.launch(
                Intent(Intent.ACTION_VIEW, url)
            )
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
