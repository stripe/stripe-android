package com.stripe.android.shoppay.webview

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader

internal class PopUpWebViewClient(
    private val assetLoader: WebViewAssetLoader,
    private val onPageLoaded: (WebView, String) -> Unit
) : WebViewClient() {
    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        request ?: return null
        return assetLoader.shouldInterceptRequest(request.url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        onPageLoaded(view, url)
    }
}
