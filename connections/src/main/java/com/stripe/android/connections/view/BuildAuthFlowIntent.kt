package com.stripe.android.connections.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.stripe.android.connections.R
import com.stripe.android.core.browser.BrowserCapabilities
import com.stripe.android.core.browser.BrowserCapabilitiesSupplier
import javax.inject.Inject

/**
 * Helper class to build the intent to launch the AuthFlow based on browser availability.
 */
internal class BuildAuthFlowIntent @Inject constructor(
    private val context: Context,
    private val browserCapabilitiesSupplier: BrowserCapabilitiesSupplier
) {

    operator fun invoke(url: String): Intent {
        val uri = Uri.parse(url)
        return when (browserCapabilitiesSupplier.get()) {
            // custom tabs available: use custom tabs intent.
            BrowserCapabilities.CustomTabs -> CustomTabsIntent.Builder()
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .build()
                .also { it.intent.data = uri }
                .intent
            // custom tabs not available - use default device browser.
            BrowserCapabilities.Unknown ->
                Intent.createChooser(
                    Intent(Intent.ACTION_VIEW, uri),
                    context.getString(R.string.link_your_account)
                )
        }
    }
}
