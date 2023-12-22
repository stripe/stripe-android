package com.stripe.hcaptcha.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RestrictTo
import com.stripe.hcaptcha.HCaptchaError
import com.stripe.hcaptcha.HCaptchaException
import com.stripe.hcaptcha.HCaptchaStateListener
import com.stripe.hcaptcha.IHCaptchaVerifier
import com.stripe.hcaptcha.config.HCaptchaConfig
import com.stripe.hcaptcha.config.HCaptchaInternalConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class HCaptchaWebViewHelper(
    handler: Handler,
    private val context: Context,
    internal val config: HCaptchaConfig,
    private val internalConfig: HCaptchaInternalConfig,
    private val captchaVerifier: IHCaptchaVerifier,
    internal val listener: HCaptchaStateListener,
    internal val webView: WebView
) {
    init {
        setupWebView(handler)
    }

    /**
     * General setup for the webview:
     * * enables javascript to be able to load and execute hcaptcha api.js
     * * loads custom html page to display challenge and/or checkbox
     */
    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun setupWebView(handler: Handler) {
        val jsInterface = HCaptchaJSInterface(handler, config, captchaVerifier)
        val debugInfo = HCaptchaDebugInfo(context)

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.loadWithOverviewMode = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.setGeolocationEnabled(false)
        settings.allowFileAccess = false
        settings.allowContentAccess = false

        webView.webViewClient = HCaptchaWebClient(handler, listener)
        webView.webChromeClient = HCaptchaWebChromeClient()
        webView.setBackgroundColor(Color.TRANSPARENT)
        if (config.disableHardwareAcceleration) {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        webView.addJavascriptInterface(jsInterface, HCaptchaJSInterface.JS_INTERFACE_TAG)
        webView.addJavascriptInterface(debugInfo, HCaptchaDebugInfo.JS_INTERFACE_TAG)
        webView.loadDataWithBaseURL(config.host, internalConfig.htmlProvider(), "text/html", "UTF-8", null)
    }

    fun destroy() {
        webView.removeJavascriptInterface(HCaptchaJSInterface.JS_INTERFACE_TAG)
        webView.removeJavascriptInterface(HCaptchaDebugInfo.JS_INTERFACE_TAG)

        val parent = webView.parent
        if (parent is ViewGroup) {
            parent.removeView(webView)
        }
        webView.destroy()
    }

    fun resetAndExecute() {
        webView.loadUrl("javascript:resetAndExecute();")
    }

    fun reset() {
        webView.loadUrl("javascript:reset();")
    }

    fun shouldRetry(exception: HCaptchaException): Boolean {
        return config.retryPredicate?.let { it(config, exception) } ?: false
    }

    private inner class HCaptchaWebClient(
        private val handler: Handler,
        private val listener: HCaptchaStateListener
    ) : WebViewClient() {
        private fun stripUrl(url: String?): String =
            url?.let {
                it.split("[?#]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0] + "..."
            } ?: "null"

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            val requestUri = request.url
            if (requestUri != null && requestUri.scheme != null && requestUri.scheme == "http") {
                handler.post {
                    webView.removeJavascriptInterface(HCaptchaJSInterface.JS_INTERFACE_TAG)
                    webView.removeJavascriptInterface(HCaptchaDebugInfo.JS_INTERFACE_TAG)
                    listener.onFailure(
                        HCaptchaException(
                            HCaptchaError.INSECURE_HTTP_REQUEST_ERROR,
                            "Insecure resource $requestUri requested"
                        )
                    )
                }
            }

            return super.shouldInterceptRequest(view, request)
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            super.onReceivedHttpError(view, request, errorResponse)
            Log.d(LOG_TAG, "[webview] onReceivedHttpError")
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            Log.d(LOG_TAG, "[webview] onPageStarted ${stripUrl(url)}")
        }

        override fun onLoadResource(view: WebView?, url: String?) {
            Log.d(LOG_TAG, "[webview] onLoadResource ${stripUrl(url)}")
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            Log.d(LOG_TAG, "[webview] onPageFinished ${stripUrl(url)}")
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.d(LOG_TAG, "[webview] onReceivedError \"${error?.description}\" (${error?.errorCode})")
            }
        }
    }

    private class HCaptchaWebChromeClient : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            Log.d(LOG_TAG, "[webview] onConsoleMessage ${consoleMessage.message()}")
            return true
        }

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            Log.d(LOG_TAG, "[webview] onProgressChanged $newProgress%")
        }
    }

    private companion object {
        private const val LOG_TAG = "hCaptchaWebView"
    }
}
