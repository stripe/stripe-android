package com.stripe.android.shoppay.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

internal class EceWebView(
    context: Context,
    androidJsBridge: AndroidBridgeHelper,
    webViewClient: WebViewClient,
    webChromeClient: WebChromeClient
): WebView(context) {
    init {
        configureDefaultSettings(androidJsBridge)
        this.webViewClient = webViewClient
        this.webChromeClient = webChromeClient
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureDefaultSettings(jsBridge: AndroidBridgeHelper) {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)
//    settings.userAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1.1 Mobile/15E148 Safari/604.1"
        settings.userAgentString =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Mobile Safari/537.36"

        // JavaScript bridge interface
        addJavascriptInterface(jsBridge, BRIDGE_NAME)
    }
}