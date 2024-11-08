package com.stripe.android.connect.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.stripe.android.connect.BuildConfig
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.StripeEmbeddedComponent
import com.stripe.android.core.Logger
import com.stripe.android.core.version.StripeSdkVersion
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@OptIn(PrivateBetaConnectSDK::class)
internal class StripeConnectWebViewClient(
    private val connectComponent: StripeEmbeddedComponent,
    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG),
    private val jsonSerializer: Json = Json {
        ignoreUnknownKeys = true
    }
) : WebViewClient() {

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        initJavascriptBridge(view)
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun configureAndLoadWebView(webView: WebView) {
        webView.apply {
            webViewClient = this@StripeConnectWebViewClient
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                userAgentString = "$userAgentString - stripe-android/${StripeSdkVersion.VERSION_NAME}"
            }
            addJavascriptInterface(StripeJsInterface(), ANDROID_JS_INTERFACE)
            addJavascriptInterface(StripeJsInterfaceInternal(), ANDROID_JS_INTERNAL_INTERFACE)

            loadUrl(EmbeddedComponentManager.getInstance().getStripeURL(connectComponent))
        }
    }

    private fun initJavascriptBridge(webView: WebView) {
        webView.evaluateJavascript(
            """
                $ANDROID_JS_INTERFACE.accountSessionClaimed = (message) => {
                    $ANDROID_JS_INTERNAL_INTERFACE.accountSessionClaimed(JSON.stringify(message));
                };
                
                $ANDROID_JS_INTERFACE.pageDidLoad = (message) => {
                    $ANDROID_JS_INTERNAL_INTERFACE.pageDidLoad(JSON.stringify(message));
                };
                $ANDROID_JS_INTERFACE.fetchClientSecret = () => {
                    return new Promise((resolve, reject) => {
                        try {
                            let clientSecret = $ANDROID_JS_INTERNAL_INTERFACE.fetchClientSecret()
                            resolve(clientSecret);
                        } catch (e) {
                            reject(e);
                        }
                    });
                };
                $ANDROID_JS_INTERFACE.onSetterFunctionCalled = (message) => {
                    $ANDROID_JS_INTERNAL_INTERFACE.onSetterFunctionCalled(JSON.stringify(message));
                };
                $ANDROID_JS_INTERFACE.openSecureWebView = (message) => {
                    $ANDROID_JS_INTERNAL_INTERFACE.openSecureWebView(JSON.stringify(message));
                };
            """.trimIndent(),
            null
        )
    }

    private inner class StripeJsInterface {
        @JavascriptInterface
        fun debug(message: String) {
            logger.debug("Debug log from JS: $message")
        }

        @JavascriptInterface
        fun fetchInitParams() {
            logger.debug("InitParams fetched")
        }

        @JavascriptInterface
        fun fetchInitComponentProps() {
            logger.debug("InitComponentProps fetched")
        }
    }

    private inner class StripeJsInterfaceInternal {
        @JavascriptInterface
        fun log(message: String) {
            logger.debug("Log from JS: $message")
        }

        @JavascriptInterface
        fun onSetterFunctionCalled(message: String) {
            val setterMessage = jsonSerializer.decodeFromString<SetterMessage>(message)
            logger.debug("Setter function called: $setterMessage")
        }

        @JavascriptInterface
        fun openSecureWebView(message: String) {
            val secureWebViewData = jsonSerializer.decodeFromString<SecureWebViewMessage>(message)
            logger.debug("Open secure web view with data: $secureWebViewData")
        }

        @JavascriptInterface
        fun pageDidLoad(message: String) {
            val pageLoadMessage = jsonSerializer.decodeFromString<PageLoadMessage>(message)
            logger.debug("Page did load: $pageLoadMessage")
        }

        @JavascriptInterface
        fun accountSessionClaimed(message: String) {
            val accountSessionClaimedMessage = jsonSerializer.decodeFromString<AccountSessionClaimedMessage>(message)
            logger.debug("Account session claimed: $accountSessionClaimedMessage")
        }

        @JavascriptInterface
        fun fetchClientSecret(): String {
            return runBlocking {
                checkNotNull(EmbeddedComponentManager.getInstance().fetchClientSecret())
            }
        }
    }

    @Serializable
    data class AccountSessionClaimedMessage(
        val merchantId: String,
    )

    @Serializable
    data class PageLoadMessage(
        val pageViewId: String
    )

    @Serializable
    data class SetterMessage(
        val setter: String,
        val value: SetterMessageValue,
    )

    @Serializable
    data class SetterMessageValue(
        val elementTagName: String,
        val message: String? = null,
    )

    @Serializable
    data class SecureWebViewMessage(
        val id: String,
        val url: String
    )

    internal companion object {
        private const val ANDROID_JS_INTERFACE = "Android"
        private const val ANDROID_JS_INTERNAL_INTERFACE = "AndroidInternal"
    }
}
