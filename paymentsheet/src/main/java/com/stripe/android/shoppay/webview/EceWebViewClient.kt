package com.stripe.android.shoppay.webview

import android.webkit.WebView
import android.webkit.WebViewClient
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger

internal open class EceWebViewClient(
    private val onPageLoaded: (WebView, String) -> Unit
) : WebViewClient() {
    private val logger = Logger.getInstance(BuildConfig.DEBUG)

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        logger.info("✅ Navigation finished: $url")
        onPageLoaded(view, url)
    }

    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
        super.onPageStarted(view, url, favicon)
        logger.info("🚀 Navigation started: $url")
    }
}
