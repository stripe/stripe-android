package com.stripe.android.core.browser

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent

class BrowserLauncher(
    private val capabilities: BrowserCapabilities,
    private val statusBarColor: Int?,
    private val url: String,
    private val title: String
) {
    fun createLaunchIntent(): Intent {
        var url = Uri.parse(url)
        if (url.scheme == null) {
            url = url.buildUpon().scheme("https").build()
        }
        val shouldUseCustomTabs = capabilities == BrowserCapabilities.CustomTabs
        return if (shouldUseCustomTabs) {
            val customTabColorSchemeParams = statusBarColor?.let { statusBarColor ->
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(statusBarColor)
                    .build()
            }

            // use Custom Tabs
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .also {
                    if (customTabColorSchemeParams != null) {
                        it.setDefaultColorSchemeParams(customTabColorSchemeParams)
                    }
                }
                .build()
            customTabsIntent.intent.data = url

            Intent.createChooser(
                customTabsIntent.intent,
                title
            )
        } else {
            // use default device browser
            Intent.createChooser(
                Intent(Intent.ACTION_VIEW, url),
                title
            )
        }
    }
}