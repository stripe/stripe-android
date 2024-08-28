package com.stripe.android.stripeconnect

import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient

internal class StripeConnectWebViewClient(
    private val appearance: Appearance? = null,
): WebViewClient() {

    private var view: WebView? = null

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        this.view = view
        if (appearance != null) {
            updateAppearance(newAppearance = appearance)
        }
    }

    fun updateAppearance(newAppearance: Appearance) {
        view?.evaluateJavascript(
            """
                setTimeout(() => {
                    StripeConnect.update({
                        appearance: { variables: ${newAppearance.asJsonString()} }
                    });
                }, 100);
            """.trimIndent()
        ) { /* Empty callback */ }
    }

    @Suppress("unused")
    inner class WebLoginJsInterface {
        @JavascriptInterface
        fun log(message: String) {
            println("TODO $message") // test log from JavaScript to Kotlin
        }
    }
}