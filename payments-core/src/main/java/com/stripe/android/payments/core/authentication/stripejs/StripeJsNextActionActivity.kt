package com.stripe.android.payments.core.authentication.stripejs

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.postDelayed
import androidx.lifecycle.SavedStateHandle
import androidx.webkit.WebViewAssetLoader
import com.stripe.android.BuildConfig
import com.stripe.android.core.Logger
import java.io.BufferedReader

internal class StripeJsNextActionActivity : AppCompatActivity() {

    private var showWebView = mutableStateOf(false)

    private lateinit var webView: WebView
    private lateinit var args: StripeJsNextActionArgs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentArgs = BundleCompat.getParcelable(intent.extras ?: Bundle(), EXTRA_ARGS, StripeJsNextActionArgs::class.java)
        if (intentArgs == null) {
            Logger.getInstance(BuildConfig.DEBUG).error("StripeJsNextActionArgs not found")
            finishWithResult(StripeJsNextActionActivityResult.Failed(IllegalArgumentException("Missing arguments")))
            return
        }

        args = intentArgs
        Logger.getInstance(BuildConfig.DEBUG).info("StripeJsNextActionActivity created with publishableKey: ${args.publishableKey}")
        Logger.getInstance(BuildConfig.DEBUG).info("StripeJsNextActionActivity created with intent id: ${args.intent.id}")

        setContent {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (showWebView.value.not()) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }

                AnimatedVisibility(
                    visible = showWebView.value,
                    modifier = Modifier
                        .fillMaxSize(),
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize(),
                        factory = {
                            webView
                        },
                        update = { view ->
                            view.setBackgroundColor(Color.TRANSPARENT)
                            view.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                        }
                    )
                }
            }
        }

        setupWebView()
        loadHtmlPage()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val assetLoader = createAssetLoader()

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: android.webkit.WebResourceRequest
                ): android.webkit.WebResourceResponse? {
                    return assetLoader.shouldInterceptRequest(request.url)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view ?: return
                    url ?: return

                    if (url.contains("stripesdk")) {
                        val uri = Uri.parse(url)
                        Logger.getInstance(BuildConfig.DEBUG).info("URL: $url")
                        val redirectStatus = uri.getQueryParameter("redirect_status")
                        val paymentIntentClientSecret = uri.getQueryParameter("payment_intent_client_secret")
                        if (redirectStatus == "succeeded" && paymentIntentClientSecret != null) {
                            Logger.getInstance(BuildConfig.DEBUG).info("Next action completed successfully")
                            finishWithResult(StripeJsNextActionActivityResult.Completed(paymentIntentClientSecret))
                            return
                        } else {
                            Logger.getInstance(BuildConfig.DEBUG).error("Next action failed: $url")
                            finishWithResult(StripeJsNextActionActivityResult.Failed(Exception("Next action failed: $url")))
                            return
                        }
                    }

                    // First load native.js to set up the bridge
                    val jsBridge = assets.open("www/native_stripejs.js")
                        .bufferedReader()
                        .use(BufferedReader::readText)

                    view.evaluateJavascript(jsBridge) { result ->
                        Logger.getInstance(BuildConfig.DEBUG).info("native.js loaded: $result")

                        // After native.js loads, call initializeStripe if this is the right URL
                        if (url.contains("pay.stripe.com")) {
                            view.evaluateJavascript("initializeStripe()") {
                                Logger.getInstance(BuildConfig.DEBUG).info("initializeStripe() => $it")
                                postDelayed(1000) {
                                    showWebView.value = true
                                }.run()
                            }
                        }
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                    consoleMessage?.let { message ->
                        Logger.getInstance(BuildConfig.DEBUG).info(
                            "WebView Console [${message.messageLevel()}]: ${message.message()} " +
                                "at line ${message.lineNumber()} of ${message.sourceId()}"
                        )
                    }
                    return true
                }
            }

            addJavascriptInterface(NextActionBridge(), "androidBridge")
        }
    }

    private fun loadHtmlPage() {
//        val htmlPage = assets.open("stripejs/index.html")
//            .bufferedReader()
//            .use(BufferedReader::readText)
//        webView.loadDataWithBaseURL(null, htmlPage, "text/html", "UTF-8", null)
        webView.loadUrl("https://pay.stripe.com/assets/www/stripejs_index.html")
    }

    private fun createAssetLoader(): WebViewAssetLoader {
        return WebViewAssetLoader.Builder()
            .setDomain("pay.stripe.com")
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

    private fun initializeStripeJs() {
        webView.evaluateJavascript("initializeStripe();") {
            Logger.getInstance(true).info(it)
        }
    }

    private fun handleNextAction() {
        val clientSecret = args.intent.clientSecret
        if (clientSecret != null) {
            webView.evaluateJavascript("handleNextAction('$clientSecret');", null)
        } else {
            finishWithResult(StripeJsNextActionActivityResult.Failed(IllegalArgumentException("Missing client secret")))
        }
    }

    private fun finishWithResult(result: StripeJsNextActionActivityResult) {
        val bundle = bundleOf(
            EXTRA_RESULT to result
        )
        setResult(RESULT_COMPLETE, Intent().putExtras(bundle))
        finish()
    }

    inner class NextActionBridge {

        @JavascriptInterface
        fun getInitParams(): String {
            val result = """{"publishableKey":"${args.publishableKey}"}"""
            Logger.getInstance(BuildConfig.DEBUG).info("getInitParams() returning: $result")
            return result
        }

        @JavascriptInterface
        fun onReady() {
            runOnUiThread {
                Logger.getInstance(BuildConfig.DEBUG).info("Stripe.js initialized successfully")
                handleNextAction()
            }
        }

        @JavascriptInterface
        fun onSuccess(paymentIntentJson: String) {
            runOnUiThread {
                Logger.getInstance(BuildConfig.DEBUG).info("Next action completed successfully => $paymentIntentJson")
                finishWithResult(StripeJsNextActionActivityResult.Completed(args.intent.clientSecret!!))
            }
        }

        @JavascriptInterface
        fun onError(errorMessage: String) {
            runOnUiThread {
                Logger.getInstance(BuildConfig.DEBUG).error("Next action failed: $errorMessage")
                finishWithResult(StripeJsNextActionActivityResult.Failed(Exception(errorMessage)))
            }
        }

        @JavascriptInterface
        fun logConsole(logData: String) {
            runOnUiThread {
                try {
                    // Parse the log data from JavaScript
                    val logEntry = org.json.JSONObject(logData)
                    val level = logEntry.optString("level", "log")
                    val message = logEntry.optString("message", "")
                    val timestamp = logEntry.optLong("timestamp", System.currentTimeMillis())
                    val stackTrace = logEntry.optString("stackTrace")

                    val logMessage = "JS [$level]: $message"
                    val logger = Logger.getInstance(BuildConfig.DEBUG)

                    when (level) {
                        "error" -> {
                            if (stackTrace.isNotEmpty()) {
                                logger.error("$logMessage\nStack: $stackTrace")
                            } else {
                                logger.error(logMessage)
                            }
                        }
                        "warn" -> logger.warning(logMessage)
                        "info" -> logger.info(logMessage)
                        "debug" -> logger.debug(logMessage)
                        else -> logger.info(logMessage)
                    }
                } catch (e: Exception) {
                    Logger.getInstance(BuildConfig.DEBUG).error("Failed to parse JS log: $logData", e)
                }
            }
        }

        @JavascriptInterface
        fun ready(message: String) {
            runOnUiThread {
                Logger.getInstance(BuildConfig.DEBUG).info("JavaScript bridge ready: $message")
            }
        }
    }

    companion object {
        internal const val EXTRA_ARGS = "stripe_js_next_Action_args"
        internal const val EXTRA_RESULT = "stripe_js_next_action_result"
        internal const val RESULT_COMPLETE = 4636

        internal fun createIntent(
            context: Context,
            args: StripeJsNextActionArgs
        ): Intent {
            return Intent(context, StripeJsNextActionActivity::class.java)
                .putExtras(getBundle(args))
        }

        internal fun getBundle(args: StripeJsNextActionArgs): Bundle {
            return bundleOf(EXTRA_ARGS to args)
        }

        internal fun getArgs(savedStateHandle: SavedStateHandle): StripeJsNextActionArgs? {
            return savedStateHandle.get<StripeJsNextActionArgs>(EXTRA_ARGS)
        }
    }
}
