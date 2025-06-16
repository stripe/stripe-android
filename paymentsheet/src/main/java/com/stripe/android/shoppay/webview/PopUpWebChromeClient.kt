package com.stripe.android.shoppay.webview

import android.content.Context
import android.os.Message
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import com.stripe.android.shoppay.bridge.BridgeHandler

internal class PopUpWebChromeClient(
    private val context: Context,
    private val bridgeHandler: BridgeHandler,
    private val assetLoader: WebViewAssetLoader,
    private val setPopUpView: (WebView) -> Unit,
    private val closeWebView: () -> Unit,
    private val onPageLoaded: (WebView, String) -> Unit
) : WebChromeClient() {
    private val logger = Logger.getInstance(BuildConfig.DEBUG)
    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        logger.debug(
            "${getEmojiForLogLevel(consoleMessage.messageLevel().toString())} " +
                "[${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}] " +
                consoleMessage.message()
        )
        return true
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        result?.confirm()
        return true
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        if (resultMsg == null) return false
        val transport = (resultMsg.obj as? WebView.WebViewTransport) ?: return false
        val popupWebView = EceWebView(
            context = context,
            bridgeHandler = bridgeHandler,
            webViewClient = PopUpWebViewClient(
                assetLoader = assetLoader,
                onPageLoaded = onPageLoaded
            ),
            webChromeClient = PopUpWebChromeClient(
                context = context,
                bridgeHandler = bridgeHandler,
                assetLoader = assetLoader,
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

    override fun onCloseWindow(window: WebView?) {
        super.onCloseWindow(window)
        closeWebView()
    }
}

private fun getEmojiForLogLevel(level: String): String {
    return when (level.lowercase()) {
        "error" -> "❌"
        "warning" -> "⚠️"
        else -> "📝"
    }
}
