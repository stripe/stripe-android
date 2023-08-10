package com.stripe.android.financialconnections.browser

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

internal object BrowserUtils {

    /**
     * Get the default browser app package that will be used to open the given [Uri].
     */
    fun getBrowserPackage(context: Context, uri: Uri): String? = runCatching {
        val (_, resolveInfo: String?) = browserIntent(uri, context)
        return resolveInfo
    }.getOrNull()

    /**
     * Constructs an intent to launch the given [Uri] on a CustomTab or in a regular browser,
     * based on the default browser set for this device.
     */
    fun createBrowserIntentForUrl(context: Context, uri: Uri): Intent {
        val (browserIntent, resolveInfo: String?) = browserIntent(uri, context)
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

    private fun browserIntent(
        uri: Uri,
        context: Context
    ): Pair<Intent, String?> {
        val browserIntent = Intent(Intent.ACTION_VIEW, uri)
        val resolveInfo: String? = context.packageManager
            .resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName
        return Pair(browserIntent, resolveInfo)
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
