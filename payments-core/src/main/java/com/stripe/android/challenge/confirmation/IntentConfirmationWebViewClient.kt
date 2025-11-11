package com.stripe.android.challenge.confirmation

import android.net.http.SslError
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

internal class IntentConfirmationWebViewClient(
    private val hostUrl: String,
    private val errorHandler: WebViewErrorHandler
) : WebViewClient() {

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        if (request?.url?.toString() != hostUrl) return
        errorHandler(
            WebViewError(
                message = error?.description?.toString(),
                errorCode = error?.errorCode,
                url = request.url?.toString(),
                webViewErrorType = "generic_resource_error"
            )
        )
    }

    // Pre-23
    @Suppress("DEPRECATION")
    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        if (failingUrl != hostUrl) return
        errorHandler(
            WebViewError(
                message = description,
                errorCode = errorCode,
                url = failingUrl,
                webViewErrorType = "generic_resource_error"
            )
        )
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (request?.url?.toString() != hostUrl) return
        errorHandler(
            WebViewError(
                message = errorResponse?.reasonPhrase,
                errorCode = errorResponse?.statusCode,
                url = request.url?.toString(),
                webViewErrorType = "http_error"
            )
        )
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        super.onReceivedSslError(view, handler, error)
        handler?.cancel()
        errorHandler(
            WebViewError(
                message = "received ssl error",
                errorCode = error?.primaryError,
                url = error?.url,
                webViewErrorType = "ssl_error"
            )
        )
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        errorHandler(
            WebViewError(
                message = "render process crashed",
                errorCode = null,
                url = view?.url,
                webViewErrorType = "render_process_gone"
            )
        )
        return super.onRenderProcessGone(view, detail)
    }
}
