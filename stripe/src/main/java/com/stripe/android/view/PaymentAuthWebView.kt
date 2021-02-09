package com.stripe.android.view

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import com.stripe.android.Logger
import com.stripe.android.R
import com.stripe.android.view.PaymentAuthWebView.PaymentAuthWebViewClient.Companion.BLANK_PAGE

/**
 * A `WebView` used for authenticating payment details
 */
internal class PaymentAuthWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {
    private var webViewClient: PaymentAuthWebViewClient? = null

    init {
        configureSettings()
    }

    fun init(
        activity: Activity,
        logger: Logger,
        progressBar: ProgressBar,
        clientSecret: String,
        returnUrl: String? = null
    ) {
        val webViewClient = PaymentAuthWebViewClient(
            activity,
            logger,
            progressBar,
            clientSecret,
            returnUrl
        )
        setWebViewClient(webViewClient)
        this.webViewClient = webViewClient

        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.message()?.let {
                    logger.debug(it)
                }
                return super.onConsoleMessage(consoleMessage)
            }

            override fun onJsConfirm(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
                    .create()
                    .show()
                return true
            }
        }
    }

    override fun destroy() {
        cleanup()
        super.destroy()
    }

    // inspired by https://stackoverflow.com/a/17458577/11103900
    private fun cleanup() {
        clearHistory()
        loadBlank()
        onPause()
        removeAllViews()
        destroyDrawingCache()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureSettings() {
        settings.javaScriptEnabled = true
        settings.allowContentAccess = false
        settings.domStorageEnabled = true
    }

    private fun loadBlank() {
        webViewClient?.let {
            it.hasLoadedBlank = true
        }
        loadUrl(BLANK_PAGE)
    }

    internal class PaymentAuthWebViewClient(
        private val activity: Activity,
        private val logger: Logger,
        private val progressBar: ProgressBar,
        private val clientSecret: String,
        returnUrl: String?
    ) : WebViewClient() {
        // user-specified return URL
        private val userReturnUri: Uri? = returnUrl?.let { Uri.parse(it) }

        var completionUrlParam: String? = null
            private set

        internal var hasLoadedBlank: Boolean = false

        override fun onPageFinished(view: WebView, url: String?) {
            logger.debug("PaymentAuthWebViewClient#onPageFinished() - $url")
            super.onPageFinished(view, url)

            if (!hasLoadedBlank) {
                // hide the progress bar here because doing it in `onPageCommitVisible()`
                // potentially causes a crash
                hideProgressBar()
            }

            if (url != null && isCompletionUrl(url)) {
                onAuthCompleted()
            }
        }

        private fun hideProgressBar() {
            logger.debug("PaymentAuthWebViewClient#hideProgressBar()")
            progressBar.isGone = true
        }

        private fun isAuthenticateUrl(url: String) = isAllowedUrl(url, AUTHENTICATE_URLS)

        private fun isCompletionUrl(url: String) = isAllowedUrl(url, COMPLETION_URLS)

        private fun isAllowedUrl(url: String, allowedUrls: Set<String>): Boolean {
            for (completionUrl in allowedUrls) {
                if (url.startsWith(completionUrl)) {
                    return true
                }
            }

            return false
        }

        override fun shouldOverrideUrlLoading(view: WebView, urlString: String): Boolean {
            logger.debug("PaymentAuthWebViewClient#shouldOverrideUrlLoading() - $urlString")
            val uri = Uri.parse(urlString)
            updateCompletionUrl(uri)

            return if (isReturnUrl(uri)) {
                logger.debug("PaymentAuthWebViewClient#shouldOverrideUrlLoading() - handle return URL")
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
            logger.debug("PaymentAuthWebViewClient#openIntentScheme()")
            runCatching {
                openIntent(Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME))
            }.onFailure {
                onAuthCompleted()
            }
        }

        /**
         * See https://developer.android.com/training/basics/intents/package-visibility-use-cases
         * for more details on app-to-app interaction.
         */
        private fun openIntent(intent: Intent) {
            logger.debug("PaymentAuthWebViewClient#openIntent()")

            runCatching {
                activity.startActivity(intent)
            }.onFailure {
                if (intent.scheme != "alipays") {
                    // complete auth if the deep-link can't be opened unless it is Alipay.
                    // The Alipay web view tries to open the Alipay app as soon as it is opened
                    // irrespective of whether or not the app is installed.
                    // If this intent fails to resolve, we should still let the user
                    // continue on the mobile site.
                    onAuthCompleted()
                }
            }
        }

        private fun updateCompletionUrl(uri: Uri) {
            logger.debug("PaymentAuthWebViewClient#updateCompletionUrl()")
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
            logger.debug("PaymentAuthWebViewClient#shouldOverrideUrlLoading(WebResourceRequest)")
            return shouldOverrideUrlLoading(view, request.url.toString())
        }

        private fun isReturnUrl(uri: Uri): Boolean {
            logger.debug("PaymentAuthWebViewClient#isReturnUrl()")
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
            logger.debug("PaymentAuthWebViewClient#onAuthCompleted()")
            activity.finish()
        }

        internal companion object {
            internal const val PARAM_PAYMENT_CLIENT_SECRET = "payment_intent_client_secret"
            internal const val PARAM_SETUP_CLIENT_SECRET = "setup_intent_client_secret"

            private val AUTHENTICATE_URLS = setOf(
                "https://hooks.stripe.com/three_d_secure/authenticate"
            )

            private val COMPLETION_URLS = setOf(
                "https://hooks.stripe.com/redirect/complete/src_",
                "https://hooks.stripe.com/3d_secure/complete/tdsrc_"
            )

            private const val PARAM_RETURN_URL = "return_url"

            internal const val BLANK_PAGE = "about:blank"
        }
    }
}
