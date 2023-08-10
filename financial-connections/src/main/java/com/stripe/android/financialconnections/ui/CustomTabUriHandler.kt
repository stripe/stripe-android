package com.stripe.android.financialconnections.ui

import android.content.Context
import android.net.Uri
import androidx.compose.ui.platform.UriHandler
import com.stripe.android.financialconnections.browser.BrowserUtils

/**
 * Alternative [UriHandler] that opens uris in a Custom tab when available
 * using [BrowserUtils.createBrowserIntentForUrl].
 */
internal class CustomTabUriHandler(private val context: Context) : UriHandler {
    override fun openUri(uri: String) {
        context.startActivity(
            BrowserUtils.createBrowserIntentForUrl(
                context = context,
                uri = Uri.parse(uri)
            )
        )
    }
}
