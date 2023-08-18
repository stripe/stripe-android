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
    fun getPackageToHandleUri(context: Context, uri: Uri): String? = runCatching {
        getPackageToHandleIntent(context, uri.toIntent())
    }.getOrNull()

    /**
     * Constructs an intent to launch the given [Uri] on a CustomTab or in a regular browser,
     * based on the default browser set for this device.
     */
    fun createBrowserIntentForUrl(context: Context, uri: Uri): Intent {
        val browserIntent = uri.toIntent()
        val defaultPackage = getPackageToHandleIntent(context, browserIntent)
        return when {
            /**
             * Firefox browser has a redirect issue when launching as a custom tab.
             * @see [BANKCON-3846]
             */
            defaultPackage?.contains(FIREFOX_PACKAGE) == true -> browserIntent
            else -> createCustomTabIntent(uri)
        }
    }

    private fun Uri.toIntent(): Intent = Intent(Intent.ACTION_VIEW, this)

    private fun getPackageToHandleIntent(context: Context, intent: Intent): String? =
        context.packageManager
            .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName

    private fun createCustomTabIntent(uri: Uri): Intent = CustomTabsIntent.Builder()
        .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
        .build()
        .also { it.intent.data = uri }
        .intent

    private const val FIREFOX_PACKAGE = "org.mozilla"
}
