package com.stripe.android.payments

import android.content.ComponentName
import android.content.Context
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection

/**
 * A class to check if the device supports Custom Tabs.
 *
 * See https://developer.chrome.com/docs/android/custom-tabs/integration-guide/ for more details.
 */
internal class CustomTabsCapabilities(
    private val context: Context
) {
    fun isSupported(): Boolean {
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
        ) {
        }

        override fun onServiceDisconnected(name: ComponentName) {}
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
