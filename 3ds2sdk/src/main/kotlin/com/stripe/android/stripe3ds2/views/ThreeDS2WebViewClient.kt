package com.stripe.android.stripe3ds2.views

import android.net.Uri
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.VisibleForTesting
import java.util.Locale

/**
 * A [WebViewClient] whose purpose is to block external resources from loading and prevent
 * navigation.
 */
internal class ThreeDS2WebViewClient : WebViewClient() {
    internal var listener: OnHtmlSubmitListener? = null

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        handleFormSubmitUrl(request.url)

        return if (shouldNotInterceptUrl(request.url)) {
            super.shouldInterceptRequest(view, request)
        } else {
            WebResourceResponse(null, null, null)
        }
    }

    @VisibleForTesting
    fun shouldNotInterceptUrl(uri: Uri): Boolean {
        return URLUtil.isDataUrl(uri.toString())
    }

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        handleFormSubmitUrl(request.url)
        return true
    }

    @VisibleForTesting
    fun handleFormSubmitUrl(
        uri: Uri
    ) {
        if (uri.toString().lowercase(Locale.ENGLISH).startsWith(CHALLENGE_URL)) {
            listener?.onHtmlSubmit(uri.query)
        }
    }

    internal fun interface OnHtmlSubmitListener {
        fun onHtmlSubmit(data: String?)
    }

    companion object {
        const val CHALLENGE_URL = "https://emv3ds/challenge"
    }
}
