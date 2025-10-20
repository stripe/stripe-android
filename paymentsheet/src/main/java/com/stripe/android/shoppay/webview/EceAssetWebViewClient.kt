package com.stripe.android.shoppay.webview

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader

internal class EceAssetWebViewClient(
    private val assetLoader: WebViewAssetLoader,
    onPageLoaded: (WebView, String) -> Unit
) : EceWebViewClient(onPageLoaded) {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return request?.let {
            assetLoader.shouldInterceptRequest(it.url)
        } == null
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        return request?.let {
            assetLoader.shouldInterceptRequest(request.url)
        }
    }
}
