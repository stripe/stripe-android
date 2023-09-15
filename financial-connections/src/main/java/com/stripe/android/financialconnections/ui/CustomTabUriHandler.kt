package com.stripe.android.financialconnections.ui

import android.content.Context
import android.net.Uri
import androidx.compose.ui.platform.UriHandler
import com.stripe.android.financialconnections.browser.BrowserManager

/**
 * Alternative [UriHandler] that opens uris in a Custom tab when available
 * using [BrowserManager].
 */
internal class CustomTabUriHandler(
    private val context: Context,
    private val browserManager: BrowserManager
) : UriHandler {
    override fun openUri(uri: String) {
        context.startActivity(
            browserManager.createBrowserIntentForUrl(uri = Uri.parse(uri))
        )
    }
}
