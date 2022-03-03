package com.stripe.android.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView
import com.stripe.android.core.networking.RequestHeadersFactory
import com.stripe.android.view.PaymentAuthWebViewClient.Companion.BLANK_PAGE

/**
 * A `WebView` used for authenticating payment details
 */
internal class PaymentAuthWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {
    internal var onLoadBlank = {}

    init {
        configureSettings()
    }

    override fun destroy() {
        cleanup()
        super.destroy()
    }

    // inspired by https://stackoverflow.com/a/17458577/11103900
    private fun cleanup() {
        clearHistory()

        onLoadBlank()
        loadUrl(BLANK_PAGE)

        onPause()
        removeAllViews()
        destroyDrawingCache()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureSettings() {
        val sdkUserAgent = RequestHeadersFactory.getUserAgent()
        settings.userAgentString = "${settings.userAgentString.orEmpty()} [$sdkUserAgent]"
        settings.javaScriptEnabled = true
        settings.allowContentAccess = false
        settings.domStorageEnabled = true
    }
}
