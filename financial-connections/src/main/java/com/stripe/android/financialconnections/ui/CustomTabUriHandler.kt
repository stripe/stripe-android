package com.stripe.android.financialconnections.ui

import android.content.Context
import android.net.Uri
import androidx.compose.ui.platform.UriHandler
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl

/**
 * Alternative [UriHandler] that opens uris in a Custom tab when available
 * using [CreateBrowserIntentForUrl].
 */
internal class CustomTabUriHandler(private val context: Context) : UriHandler {
    override fun openUri(uri: String) {
        context.startActivity(
            CreateBrowserIntentForUrl(
                context = context,
                uri = Uri.parse(uri)
            )
        )
    }
}
