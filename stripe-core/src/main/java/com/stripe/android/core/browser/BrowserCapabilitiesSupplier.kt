package com.stripe.android.core.browser

import android.content.ComponentName
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection

/**
 * Supply the device's [BrowserCapabilities].
 *
 * See https://developer.chrome.com/docs/android/custom-tabs/integration-guide/ for more details
 * on Custom Tabs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BrowserCapabilitiesSupplier(
    private val context: Context
) {
    fun get(): BrowserCapabilities {
        return when {
            isCustomTabsSupported() -> BrowserCapabilities.CustomTabs
            else -> BrowserCapabilities.Unknown
        }
    }

    private fun isCustomTabsSupported(): Boolean {
        return runCatching {
            CustomTabsClient.bindCustomTabsService(
                context,
                CHROME_PACKAGE,
                NoopCustomTabsServiceConnection()
            )
        }.getOrDefault(false)
    }

    private class NoopCustomTabsServiceConnection : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(
            componentName: ComponentName,
            customTabsClient: CustomTabsClient
        ) = Unit

        override fun onServiceDisconnected(name: ComponentName) = Unit
    }

    private companion object {
        /**
         * Stable = com.android.chrome
         * Beta = com.chrome.beta
         * Dev = com.chrome.dev
         */
        private const val CHROME_PACKAGE = "com.android.chrome"
    }
}
