package com.stripe.android.shoppay.webview

import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

internal class PopUpWebViewClient(
    private val onPageLoaded: (WebView) -> Unit
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        Log.d("WebViewBridge", "url => ${request.url}")
        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        Log.d("WebViewBridge", "✅ Navigation finished: $url")

        // Inject JavaScript bridge after page loads
        onPageLoaded(view)
    }

    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
        super.onPageStarted(view, url, favicon)
        Log.d("WebViewBridge", "🚀 Navigation started: $url")
    }

}
