package com.stripe.android.shoppay.webview

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger

internal class PopUpWebViewClient(
    private val assetLoader: WebViewAssetLoader,
    private val onPageLoaded: (WebView, String) -> Unit
) : WebViewClient() {
    private val logger = Logger.getInstance(BuildConfig.DEBUG)

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        request ?: return null
        return assetLoader.shouldInterceptRequest(request.url)
    }

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
