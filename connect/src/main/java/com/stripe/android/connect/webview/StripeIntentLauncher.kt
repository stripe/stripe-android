package com.stripe.android.connect.webview

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

internal interface StripeIntentLauncher {
    /**
     * Launches [uri] in a secure external Android Custom Tab.
     */
    fun launchSecureExternalWebTab(context: Context, uri: Uri)
}

internal class StripeIntentLauncherImpl : StripeIntentLauncher {
    override fun launchSecureExternalWebTab(context: Context, uri: Uri) {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, uri)
    }
}
