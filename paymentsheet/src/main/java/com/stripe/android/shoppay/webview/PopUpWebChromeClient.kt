package com.stripe.android.shoppay.webview

import android.content.Context
import android.os.Message
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView

internal class PopUpWebChromeClient(
    private val context: Context,
    private val androidJsBridge: AndroidBridgeHelper,
    private val setPopUpView: (WebView) -> Unit,
    private val closeWebView: (WebView?) -> Unit,
    private val onPageLoaded: (WebView) -> Unit
) : WebChromeClient() {
    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        Log.d(
            "WebConsole",
            "${getEmojiForLogLevel(consoleMessage.messageLevel().toString())} " +
                "[${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}] " +
                "${consoleMessage.message()}"
        )
        return true
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        Log.d("WebViewBridge", "Alert: $message")
        result?.confirm()
        return true
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        if (resultMsg != null) {
            val transport = resultMsg.obj as WebView.WebViewTransport
            val popupWebView = EceWebView(
                context = context,
                androidJsBridge = androidJsBridge,
                webViewClient = PopUpWebViewClient(
                    onPageLoaded = onPageLoaded
                ),
                webChromeClient = PopUpWebChromeClient(
                    context = context,
                    androidJsBridge = androidJsBridge,
                    setPopUpView = setPopUpView,
                    closeWebView = closeWebView,
                    onPageLoaded = onPageLoaded
                )
            )

            transport.webView = popupWebView
            resultMsg.sendToTarget()
            setPopUpView(popupWebView)
            return true
        }
        return false
    }

    override fun onCloseWindow(window: WebView?) {
        super.onCloseWindow(window)
        closeWebView(window)
    }
}

private fun getEmojiForLogLevel(level: String): String {
    return when (level.lowercase()) {
        "error" -> "❌"
        "warning" -> "⚠️"
        "log" -> "📝"
        else -> "📝" // Default to log emoji
    }
}
