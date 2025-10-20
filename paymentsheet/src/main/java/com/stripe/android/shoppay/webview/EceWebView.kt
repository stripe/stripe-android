package com.stripe.android.shoppay.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

@SuppressLint("ViewConstructor")
internal class EceWebView(
    context: Context,
    webViewClient: WebViewClient,
    webChromeClient: WebChromeClient
) : WebView(context) {
    init {
        configureDefaultSettings()
        this.webViewClient = webViewClient
        this.webChromeClient = webChromeClient
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureDefaultSettings() {
        settings.javaScriptEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)
        settings.userAgentString = FakeUserAgent
    }
}

internal const val BRIDGE_NAME = "androidBridge"
private const val FakeUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 " +
    "(KHTML, like Gecko) Version/18.1.1 Mobile/15E148 Safari/604.1"
