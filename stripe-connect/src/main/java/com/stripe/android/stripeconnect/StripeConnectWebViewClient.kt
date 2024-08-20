package com.stripe.android.stripeconnect

import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient

internal class StripeConnectWebViewClient: WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        view?.evaluateJavascript(
            """
            window.webkit = {
                messageHandlers: {
//                    fetchClientSecret: { postMessage: (payload) => { return Android.fetchClientSecret() } },
//                    fetchInitParams: { postMessage: (payload) => { return {} } }
                }
            };
            """.trimIndent(),
            null
        )
    }

    @Suppress("unused")
    inner class WebLoginJsInterface {
        @JavascriptInterface
        fun log(message: String) {
            println("TODO $message") // test log from JavaScript to Kotlin
        }
    }
}