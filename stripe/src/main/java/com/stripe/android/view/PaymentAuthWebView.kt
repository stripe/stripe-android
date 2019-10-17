package com.stripe.android.view

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.webkit.URLUtil
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
        Log.d(TAG, "PaymentAuthWebView#init()")
        webViewClient = PaymentAuthWebViewClient(activity, activity.packageManager, progressBar,
            clientSecret, returnUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureSettings() {
        settings.javaScriptEnabled = true
        settings.allowContentAccess = false
        settings.domStorageEnabled = true
    }

    override fun destroy() {
        cleanup()
        super.destroy()
    }

    // inspired by https://stackoverflow.com/a/17458577/11103900
    private fun cleanup() {
        clearHistory()
        loadUrl("about:blank")
        onPause()
        removeAllViews()
        destroyDrawingCache()
    }

    internal class PaymentAuthWebViewClient(
        private val activity: Activity,
        private val packageManager: PackageManager,
        private val progressBar: ProgressBar,
        private val clientSecret: String,
        returnUrl: String?
    ) : WebViewClient() {
        // user-specified return URL
        private val userReturnUri: Uri? = if (returnUrl != null) Uri.parse(returnUrl) else null

        var completionUrlParam: String? = null
            private set

        override fun onPageFinished(view: WebView, url: String?) {
            Log.d(TAG, "PaymentAuthWebViewClient#onPageFinished() - $url")
            super.onPageFinished(view, url)

            // hide the progress bar here because doing it in `onPageCommitVisible()` potentially
            // causes a crash
            progressBar.visibility = View.GONE

            Log.d(TAG, "PaymentAuthWebViewClient#onPageFinished() - hide progress bar")

            if (url != null && isCompletionUrl(url)) {
                onAuthCompleted()
            }
        }

        private fun isAuthenticateUrl(url: String) = isWhiteListedUrl(url, AUTHENTICATE_URLS)

        private fun isCompletionUrl(url: String) = isWhiteListedUrl(url, COMPLETION_URLS)

        private fun isWhiteListedUrl(url: String, whitelistedUrls: Set<String>): Boolean {
            for (completionUrl in whitelistedUrls) {
                if (url.startsWith(completionUrl)) {
                    return true
                }
            }

            return false
        }

        override fun shouldOverrideUrlLoading(view: WebView, urlString: String): Boolean {
            Log.d(TAG, "PaymentAuthWebViewClient#shouldOverrideUrlLoading() - $urlString")
            val uri = Uri.parse(urlString)
            updateCompletionUrl(uri)

            return if (isReturnUrl(uri)) {
                Log.d(TAG, "PaymentAuthWebViewClient#shouldOverrideUrlLoading() - handle return URL")
                onAuthCompleted()
                true
            } else if ("intent".equals(uri.scheme, ignoreCase = true)) {
                openIntentScheme(uri)
                true
            } else if (!URLUtil.isNetworkUrl(uri.toString())) {
                // Non-network URLs are likely deep-links into banking apps. If the deep-link can be
                // opened via an Intent, start it. Otherwise, stop the authentication attempt.
                openIntent(Intent(Intent.ACTION_VIEW, uri))
                true
            } else {
                super.shouldOverrideUrlLoading(view, urlString)
            }
        }

        private fun openIntentScheme(uri: Uri) {
            Log.d(TAG, "PaymentAuthWebViewClient#openIntentScheme() - $uri")
            try {
                openIntent(Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME))
            } catch (e: Exception) {
                onAuthCompleted()
            }
        }

        private fun openIntent(intent: Intent) {
            Log.d(TAG, "PaymentAuthWebViewClient#openIntent() - $intent")
            if (intent.resolveActivity(packageManager) != null) {
                activity.startActivity(intent)
            } else {
                // complete auth if the deep-link can't be opened
                onAuthCompleted()
            }
        }

        private fun updateCompletionUrl(uri: Uri) {
            Log.d(TAG, "PaymentAuthWebViewClient#updateCompletionUrl() - $uri")
            val returnUrlParam = if (isAuthenticateUrl(uri.toString())) {
                uri.getQueryParameter(PARAM_RETURN_URL)
            } else {
                null
            }

            if (!returnUrlParam.isNullOrBlank()) {
                completionUrlParam = returnUrlParam
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            return shouldOverrideUrlLoading(view, request.url.toString())
        }

        private fun isReturnUrl(uri: Uri): Boolean {
            Log.d(TAG, "PaymentAuthWebViewClient#isReturnUrl() - $uri")
            when {
                isPredefinedReturnUrl(uri) -> return true

                // If the `userReturnUri` is known, look for URIs that match it.
                userReturnUri != null -> {
                    Log.d(TAG, "PaymentAuthWebViewClient#isReturnUrl() - userReturnUri is known")
                    return userReturnUri.scheme != null &&
                        userReturnUri.scheme == uri.scheme &&
                        userReturnUri.host != null &&
                        userReturnUri.host == uri.host
                }
                else -> {
                    Log.d(TAG, "PaymentAuthWebViewClient#isReturnUrl() - userReturnUri is unknown")
                    // Skip opaque (i.e. non-hierarchical) URIs
                    if (uri.isOpaque) {
                        return false
                    }

                    // If the `userReturnUri` is unknown, look for URIs that contain a
                    // `payment_intent_client_secret` or `setup_intent_client_secret`
                    // query parameter, and check if its values matches the given `clientSecret`
                    // as a query parameter.
                    val paramNames = uri.queryParameterNames.toSet()
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
            Log.d(TAG, "PaymentAuthWebViewClient#onAuthCompleted()")
            activity.finish()
        }

        companion object {
            const val PARAM_PAYMENT_CLIENT_SECRET = "payment_intent_client_secret"
            const val PARAM_SETUP_CLIENT_SECRET = "setup_intent_client_secret"

            private val AUTHENTICATE_URLS = setOf(
                "https://hooks.stripe.com/three_d_secure/authenticate"
            )

            private val COMPLETION_URLS = setOf(
                "https://hooks.stripe.com/redirect/complete/src_",
                "https://hooks.stripe.com/3d_secure/complete/tdsrc_"
            )

            private const val PARAM_RETURN_URL = "return_url"
        }
    }

    companion object {
        internal const val TAG: String = "StripeSdk"
    }
}
