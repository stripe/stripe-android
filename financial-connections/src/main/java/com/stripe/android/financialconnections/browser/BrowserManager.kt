package com.stripe.android.financialconnections.browser

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import javax.inject.Inject

internal class BrowserManager @Inject constructor(
    val context: Application
) {

    /**
     * Returns `true` if there's an app that can open an https url.
     */
    fun canOpenHttpsUrl(): Boolean = getPackageToHandleUri(Uri.parse("https://")) != null

    /**
     * Get the default browser app package that will be used to open the given [Uri].
     */
    fun getPackageToHandleUri(uri: Uri): String? = runCatching {
        getPackageToHandleIntent(uri.toIntent())
    }.getOrNull()

    /**
     * Constructs an intent to launch the given [Uri] on a CustomTab or in a regular browser,
     * based on the default browser set for this device.
     */
    fun createBrowserIntentForUrl(uri: Uri): Intent {
        val browserIntent = uri.toIntent()
        val defaultPackage = getPackageToHandleIntent(browserIntent)
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

    private fun getPackageToHandleIntent(intent: Intent): String? =
        context.packageManager
            .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName

    private fun createCustomTabIntent(uri: Uri): Intent = CustomTabsIntent.Builder()
        .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
        .build()
        .also { it.intent.data = uri }
        .intent
}

private const val FIREFOX_PACKAGE = "org.mozilla"
