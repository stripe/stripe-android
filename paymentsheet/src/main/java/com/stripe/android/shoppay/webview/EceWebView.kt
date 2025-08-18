package com.stripe.android.shoppay.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.stripe.android.shoppay.bridge.ShopPayBridgeHandler

@SuppressLint("ViewConstructor")
internal class EceWebView(
    context: Context,
    bridgeHandler: ShopPayBridgeHandler,
    webViewClient: WebViewClient,
    webChromeClient: WebChromeClient
) : WebView(context) {
    init {
        configureDefaultSettings(bridgeHandler)
        this.webViewClient = webViewClient
        this.webChromeClient = webChromeClient
    }

    override fun evaluateJavascript(script: String, resultCallback: ValueCallback<String>?) {
        super.evaluateJavascript(script, resultCallback)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureDefaultSettings(bridgeHandler: ShopPayBridgeHandler) {
        settings.javaScriptEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)
        settings.userAgentString = FakeUserAgent

        addJavascriptInterface(bridgeHandler, BRIDGE_NAME)
    }
}

internal const val BRIDGE_NAME = "androidBridge"
private const val FakeUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 " +
    "(KHTML, like Gecko) Version/18.1.1 Mobile/15E148 Safari/604.1"
