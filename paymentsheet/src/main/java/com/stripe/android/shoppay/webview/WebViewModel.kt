package com.stripe.android.shoppay.webview

import android.content.Context
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader

internal class WebViewModel : ViewModel() {
    val androidJsBridge = AndroidBridgeHelper()
    private val _webView = MutableStateFlow<WebView?>(null)
    val webView: StateFlow<WebView?> = _webView

    private val _popupWebView = MutableStateFlow<WebView?>(null)
    val popupWebView: StateFlow<WebView?> = _popupWebView

    private val _showPopup = MutableStateFlow(false)
    val showPopup: StateFlow<Boolean> = _showPopup

    fun setWebView(webView: WebView) {
        webView.loadUrl("https://appassets.androidplatform.net/assets/www/index.html")
        _webView.value = webView
    }

    fun setPopupWebView(webView: WebView?) {
        _popupWebView.value = webView
        _showPopup.value = webView != null
    }

    fun closePopup() {
        _popupWebView.value = null
        _showPopup.value = false
    }

    fun assetLoader(context: Context): WebViewAssetLoader {
        return WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }

    fun injectJavaScriptBridge(webView: WebView?) {
        webView ?: return
        val jsBridge = webView.context.assets.open("www/native.js")
            .bufferedReader()
            .use(BufferedReader::readText)

        webView.evaluateJavascript(jsBridge, null)
    }
}
