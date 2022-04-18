package com.stripe.android.connections.presentation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Constructs an intent to launch the given [Uri] on a CustomTab or in a regular browser,
 * based on the default browser set for this device.
 */
object CreateBrowserIntentForUrl {

    operator fun invoke(context: Context, uri: Uri): Intent {
        val browserIntent = Intent(Intent.ACTION_VIEW, uri)
        val resolveInfo: String? = context.packageManager
            .resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName
        return when {
            /**
             * Firefox browser has a redirect issue when launching as a custom tab.
             * @see [BANKCON-3846]
             */
            resolveInfo?.contains(FIREFOX_PACKAGE) == true -> browserIntent
            resolveInfo?.contains(CHROME_PACKAGE) == true -> createCustomTabIntent(uri)
            else -> createCustomTabIntent(uri)
        }
    }

    private fun createCustomTabIntent(uri: Uri): Intent {
        return CustomTabsIntent.Builder()
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .build()
            .also { it.intent.data = uri }
            .intent
    }

    private const val FIREFOX_PACKAGE = "org.mozilla"
    private const val CHROME_PACKAGE = "com.android.chrome"
}
