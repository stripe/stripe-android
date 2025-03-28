package com.stripe.android.connect

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.browser.customtabs.CustomTabsIntent

/**
 * An activity used to (re)open the app via redirect deep link, e.g. stripe-connect//com.example.app.
 * It is designed to resume where the user was left off after opening a Chrome Custom Tab.
 *
 * Features:
 *  - Opening the redirect deep link will close the Custom Tab and resume the launching activity,
 *    even while the app is in the foreground
 *  - Custom Tab is opened in the same task as the launching activity, so:
 *    - the launching activity is *always* resumed after the Custom Tab is closed, no matter how
 *      it was closed
 *    - no additional task is added to the Android "Recents" screen
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StripeConnectRedirectActivity : Activity() {
    private var didPause = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val customTabUrl = intent.getStringExtra(EXTRA_CUSTOM_TAB_URL)
        if (customTabUrl != null) {
            // Open the Custom Tab, but don't finish. The activity needs to be under the
            // Custom Tab activity so when the redirect is received, the task is cleared
            // via the combination of:
            //  - this activity having launchMode="singleTask"
            //  - intent flags FLAG_ACTIVITY_SINGLE_TOP and FLAG_ACTIVITY_CLEAR_TOP
            // With the task cleared, the activity finishes in `onNewIntent()`
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(this, Uri.parse(customTabUrl))
        } else {
            // Shouldn't happen under normal circumstances -- just finish.
            finish()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // See comment in `onCreate()`.
        finish()
    }

    override fun onResume() {
        super.onResume()
        // When launching a Custom Tab, this activity will briefly resume before immediately
        // pausing when the Custom Tab activity opens. We don't want to `finish()` in this
        // initial case (see comment in `onCreate()`).
        //
        // However, we *do* want to `finish()` in subsequent resumes because it means the
        // Custom Tab has closed.
        if (didPause) {
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        // See `onResume()`.
        didPause = true
    }

    internal companion object {
        private const val EXTRA_CUSTOM_TAB_URL = "custom_tab_url"

        fun customTabIntent(context: Context, url: String): Intent {
            return Intent(context, StripeConnectRedirectActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_CUSTOM_TAB_URL, url)
        }
    }
}
