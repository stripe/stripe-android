package com.stripe.android.financialconnections.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.UriHandler
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl

/**
 * Alternative [UriHandler] that opens uris in a Custom tab when available
 * using [CreateBrowserIntentForUrl].
 */
internal class CustomTabUriHandler(private val context: Context) : UriHandler {
    override fun openUri(uri: String) {
        val browserIntent = CreateBrowserIntentForUrl(
            context = context,
            uri = Uri.parse(uri)
        )
        browserIntent?.let {
            context.startActivity(it)
        } ?: kotlin.run {
            Toast.makeText(context, "No application can handle this request. Please install a web browser.", Toast.LENGTH_LONG).show()
        }
    }
}
