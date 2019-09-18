package com.stripe.android.view

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar

/**
 * A `WebView` used for authenticating payment details
 */
internal class PaymentAuthWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(createContext(context), attrs, defStyleAttr) {
    private var webViewClient: PaymentAuthWebViewClient? = null

    init {
        configureSettings()
    }

    fun init(
        activity: Activity,
        progressBar: ProgressBar,
        clientSecret: String,
        returnUrl: String?
    ) {
        webViewClient = PaymentAuthWebViewClient(activity, activity.packageManager, progressBar,
            clientSecret, returnUrl)
        setWebViewClient(webViewClient)
    }

    fun onForegrounded() {
        if (webViewClient?.hasOpenedApp == true) {
            // If another app was opened, assume it was a bank app where payment authentication
            // was completed. Upon foregrounding this screen, load the completion URL.
            webViewClient?.completionUrlParam?.let {
                loadUrl(it)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureSettings() {
        settings.javaScriptEnabled = true
        settings.allowContentAccess = false
        settings.domStorageEnabled = true
    }

    companion object {
        /**
         * Fix for crash in API 21 and 22
         *
         * See <a href="https://stackoverflow.com/q/41025200/">https://stackoverflow.com/q/41025200/</a>
         * for more context.
         */
        fun createContext(context: Context): Context {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                context.createConfigurationContext(Configuration())
            } else {
                context
            }
        }
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

        // true if another app was opened from this WebView
        var hasOpenedApp: Boolean = false
            private set

        override fun onPageCommitVisible(view: WebView, url: String) {
            super.onPageCommitVisible(view, url)
            hideProgressBar()
        }

        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
                // hide the progress bar here because `onPageCommitVisible()`
                // is not called in API 22 and below
                hideProgressBar()
            }
            if (url != null && isCompletionUrl(url)) {
                onAuthCompleted()
            }
        }

        private fun hideProgressBar() {
            progressBar.visibility = View.GONE
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
            val uri = Uri.parse(urlString)
            updateCompletionUrl(uri)

            return if (isReturnUrl(uri)) {
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
            try {
                openIntent(Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME))
            } catch (e: Exception) {
                onAuthCompleted()
            }
        }

        private fun openIntent(intent: Intent) {
            if (intent.resolveActivity(packageManager) != null) {
                hasOpenedApp = true
                activity.startActivity(intent)
            } else {
                // complete auth if the deep-link can't be opened
                onAuthCompleted()
            }
        }

        private fun updateCompletionUrl(uri: Uri) {
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
            when {
                isPredefinedReturnUrl(uri) -> return true

                // If the `userReturnUri` is known, look for URIs that match it.
                userReturnUri != null ->
                    return userReturnUri.scheme != null &&
                        userReturnUri.scheme == uri.scheme &&
                        userReturnUri.host != null &&
                        userReturnUri.host == uri.host
                else -> {
                    // Skip opaque (i.e. non-hierarchical) URIs
                    if (uri.isOpaque) {
                        return false
                    }

                    // If the `userReturnUri` is unknown, look for URIs that contain a
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
}
