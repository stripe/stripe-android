package com.stripe.android.paymentsheet.paymentdatacollection.polling

import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.stripe.android.core.Logger
import com.stripe.android.view.PaymentAuthWebViewClient

@Composable
fun PayNowWebView(
    url: String,
    clientSecret: String,
    onClose: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = PaymentAuthWebViewClient(
                        logger = Logger.noop(), // TODO: add logger?
                        clientSecret = clientSecret,
                        returnUrl = null,
                        activityStarter = {},
                        activityFinisher = { _ -> onClose() },
                    )
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Close button overlay
        Button(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text("Close")
        }
    }
}
