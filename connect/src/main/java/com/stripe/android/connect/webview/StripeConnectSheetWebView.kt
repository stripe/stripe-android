package com.stripe.android.connect.webview

import android.annotation.SuppressLint
import android.app.Application
import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.stripe.android.connect.util.findActivity
import com.stripe.android.core.version.StripeSdkVersion
import java.io.ByteArrayInputStream

/**
 * Holds a window that an inline component's web view opened with `window.open()`. Connect.js in the component's
 * web view draws a dialog into this window's document, and the SDK shows it in a native sheet.
 *
 * It has no JavaScript interface, so its content can never message the SDK, and it can't navigate away from
 * the first document it loads. Like [StripeConnectWebView], it's created with the application context so it
 * can outlive an activity across configuration changes.
 */
@SuppressLint("ViewConstructor")
internal class StripeConnectSheetWebView private constructor(
    private val mutableContext: MutableContextWrapper,
    private val listener: Listener,
) : WebView(mutableContext) {

    constructor(application: Application, listener: Listener) : this(
        mutableContext = MutableContextWrapper(application),
        listener = listener,
    )

    interface Listener {
        /**
         * Called with the window's first document. Returns true to load it as a sheet, or false to block it.
         */
        fun onWindowNavigated(webView: StripeConnectSheetWebView, url: Uri): Boolean

        /**
         * Returns true if [url] could be loaded as the window's first document. Called off the main thread.
         */
        fun isSheetUrl(url: Uri): Boolean

        /**
         * Called when the window tries to leave its first document, which is always blocked.
         */
        fun onNavigationBlocked(webView: StripeConnectSheetWebView, url: Uri, isUserGesture: Boolean)

        /**
         * Called when the window's opener calls `window.close()`.
         */
        fun onWindowClosed(webView: StripeConnectSheetWebView)

        fun onShowFileChooser(
            webView: StripeConnectSheetWebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: WebChromeClient.FileChooserParams,
        ): Boolean

        fun onPermissionRequest(webView: StripeConnectSheetWebView, request: PermissionRequest)
    }

    @Volatile
    private var documentUrl: Uri? = null
    private var didNavigate = false

    init {
        settings.apply {
            @SuppressLint("SetJavaScriptEnabled")
            javaScriptEnabled = true

            domStorageEnabled = true
            userAgentString = "$userAgentString - stripe-android/${StripeSdkVersion.VERSION_NAME}"
            mediaPlaybackRequiresUserGesture = false
        }
        webViewClient = SheetWebViewClient()
        webChromeClient = SheetWebChromeClient()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        findActivity()?.let { mutableContext.baseContext = it }
    }

    override fun onDetachedFromWindow() {
        mutableContext.baseContext = mutableContext.applicationContext
        super.onDetachedFromWindow()
    }

    private fun allowNavigation(url: Uri, isUserGesture: Boolean): Boolean {
        val currentUrl = documentUrl
        if (currentUrl == null) {
            if (didNavigate) return false
            didNavigate = true
            if (!listener.onWindowNavigated(this, url)) return false
            documentUrl = url
            return true
        }
        if (url.withoutFragment() == currentUrl.withoutFragment()) return true
        listener.onNavigationBlocked(this, url, isUserGesture)
        return false
    }

    private fun isAllowedDocument(url: Uri): Boolean {
        val currentUrl = documentUrl
        return if (currentUrl == null) {
            listener.isSheetUrl(url)
        } else {
            url.withoutFragment() == currentUrl.withoutFragment()
        }
    }

    private inner class SheetWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!request.isForMainFrame) return false
            return !allowNavigation(request.url, request.hasGesture())
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            // A window's first navigation doesn't always go through shouldOverrideUrlLoading.
            if (url != ABOUT_BLANK && !allowNavigation(Uri.parse(url), isUserGesture = false)) {
                view.stopLoading()
            }
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            if (!request.isForMainFrame || isAllowedDocument(request.url)) return null
            return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
        }
    }

    private inner class SheetWebChromeClient : WebChromeClient() {
        override fun onCloseWindow(window: WebView) {
            listener.onWindowClosed(this@StripeConnectSheetWebView)
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            return listener.onShowFileChooser(this@StripeConnectSheetWebView, filePathCallback, fileChooserParams)
        }

        override fun onPermissionRequest(request: PermissionRequest) {
            listener.onPermissionRequest(this@StripeConnectSheetWebView, request)
        }
    }

    private companion object {
        const val ABOUT_BLANK = "about:blank"

        fun Uri.withoutFragment(): Uri = buildUpon().fragment(null).build()
    }
}
