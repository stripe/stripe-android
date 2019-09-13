package com.stripe.android.view

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar

/**
 * A `WebView` used for authenticating payment details
 */
internal class PaymentAuthWebView : WebView {

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        configureSettings()
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        configureSettings()
    }

    fun init(
        activity: Activity,
        progressBar: ProgressBar,
        clientSecret: String,
        returnUrl: String?
    ) {
        webViewClient = PaymentAuthWebViewClient(activity, progressBar, clientSecret, returnUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureSettings() {
        settings.javaScriptEnabled = true
        settings.allowContentAccess = false
        settings.domStorageEnabled = true
    }

    internal class PaymentAuthWebViewClient(
        private val activity: Activity,
        private val progressBar: ProgressBar,
        private val clientSecret: String,
        returnUrl: String?
    ) : WebViewClient() {
        private val returnUrl: Uri? = if (returnUrl != null) Uri.parse(returnUrl) else null

        override fun onPageCommitVisible(view: WebView, url: String) {
            super.onPageCommitVisible(view, url)
            progressBar.visibility = View.GONE
        }

        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            if (url != null && isCompletionUrl(url)) {
                onAuthCompleted()
            }
        }

        private fun isCompletionUrl(url: String): Boolean {
            for (completionUrl in COMPLETION_URLS) {
                if (url.startsWith(completionUrl)) {
                    return true
                }
            }

            return false
        }

        override fun shouldOverrideUrlLoading(view: WebView, urlString: String): Boolean {
            val uri = Uri.parse(urlString)
            if (isReturnUrl(uri)) {
                onAuthCompleted()
                return true
            }

            return super.shouldOverrideUrlLoading(view, urlString)
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            return shouldOverrideUrlLoading(view, request.url.toString())
        }

        private fun isReturnUrl(uri: Uri): Boolean {
            when {
                isPredefinedReturnUrl(uri) -> return true

                // If the `returnUrl` is known, look for URIs that match it.
                returnUrl != null ->
                    return returnUrl.scheme != null &&
                        returnUrl.scheme == uri.scheme &&
                        returnUrl.host != null &&
                        returnUrl.host == uri.host
                else -> {
                    // Skip opaque (i.e. non-hierarchical) URIs
                    if (uri.isOpaque) {
                        return false
                    }

                    // If the `returnUrl` is unknown, look for URIs that contain a
                    // `payment_intent_client_secret` or `setup_intent_client_secret`
                    // query parameter, and check if its values matches the given `clientSecret`
                    // as a query parameter.
                    val paramNames = uri.queryParameterNames
                    val clientSecret = when {
                        paramNames.contains(PARAM_PAYMENT_CLIENT_SECRET) ->
                            uri.getQueryParameter(PARAM_PAYMENT_CLIENT_SECRET)
                        paramNames.contains(PARAM_SETUP_CLIENT_SECRET) ->
                            uri.getQueryParameter(PARAM_SETUP_CLIENT_SECRET)
                        else -> null
                    }
                    return this.clientSecret == clientSecret
                }
            }
        }

        // pre-defined return URLs
        private fun isPredefinedReturnUrl(uri: Uri): Boolean {
            return "stripejs://use_stripe_sdk/return_url" == uri.toString()
        }

        private fun onAuthCompleted() {
            activity.finish()
        }

        companion object {
            const val PARAM_PAYMENT_CLIENT_SECRET = "payment_intent_client_secret"
            const val PARAM_SETUP_CLIENT_SECRET = "setup_intent_client_secret"

            private val COMPLETION_URLS = setOf(
                "https://hooks.stripe.com/redirect/complete/src_",
                "https://hooks.stripe.com/3d_secure/complete/tdsrc_"
            )
        }
    }
}
