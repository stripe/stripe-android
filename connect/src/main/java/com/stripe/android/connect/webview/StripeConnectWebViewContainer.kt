package com.stripe.android.connect.webview

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.os.Build
import android.view.LayoutInflater
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.connect.BuildConfig
import com.stripe.android.connect.ComponentListenerDelegate
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.StripeEmbeddedComponent
import com.stripe.android.connect.StripeEmbeddedComponentListener
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.databinding.StripeConnectWebviewBinding
import com.stripe.android.connect.webview.serialization.AccountSessionClaimedMessage
import com.stripe.android.connect.webview.serialization.ConnectInstanceJs
import com.stripe.android.connect.webview.serialization.ConnectJson
import com.stripe.android.connect.webview.serialization.PageLoadMessage
import com.stripe.android.connect.webview.serialization.SecureWebViewMessage
import com.stripe.android.connect.webview.serialization.SetterFunctionCalledMessage
import com.stripe.android.connect.webview.serialization.toJs
import com.stripe.android.core.Logger
import com.stripe.android.core.version.StripeSdkVersion
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface StripeConnectWebViewContainer<Listener : StripeEmbeddedComponentListener> {
    /**
     * Initializes the [EmbeddedComponentManager] and listener to use for this view.
     * Must be called when this view is created via XML.
     * Cannot be called more than once per instance.
     */
    fun initialize(
        embeddedComponentManager: EmbeddedComponentManager,
        listener: Listener?,
    )
}

@OptIn(PrivateBetaConnectSDK::class)
internal interface StripeConnectWebViewContainerInternal {
    /**
     * Update the appearance of the Connect instance.
     */
    fun updateConnectInstance(appearance: Appearance)

    /**
     * Load the given URL in the WebView.
     */
    fun loadUrl(url: String)
}

@OptIn(PrivateBetaConnectSDK::class)
internal class StripeConnectWebViewContainerImpl<Listener : StripeEmbeddedComponentListener>(
    val embeddedComponent: StripeEmbeddedComponent,
    embeddedComponentManager: EmbeddedComponentManager?,
    listener: Listener?,
    private val listenerDelegate: ComponentListenerDelegate<Listener>,
    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG),
) : StripeConnectWebViewContainer<Listener>,
    StripeConnectWebViewContainerInternal {

    private var viewBinding: StripeConnectWebviewBinding? = null
    private val webView get() = viewBinding?.stripeWebView

    @VisibleForTesting
    internal val stripeWebViewClient = StripeConnectWebViewClient()

    @VisibleForTesting
    internal val stripeWebChromeClient = StripeWebChromeClient()

    private var controller: StripeConnectWebViewContainerController<Listener>? = null

    init {
        if (embeddedComponentManager != null) {
            initialize(embeddedComponentManager, listener)
        }
    }

    internal fun initializeView(view: FrameLayout) {
        val viewBinding = StripeConnectWebviewBinding.inflate(
            LayoutInflater.from(view.context),
            view
        )
            .also { this.viewBinding = it }
        initializeWebView(viewBinding.stripeWebView)
        bindViewToController()
    }

    @VisibleForTesting
    internal fun initializeWebView(webView: WebView) {
        with(webView) {
            webViewClient = stripeWebViewClient
            webChromeClient = stripeWebChromeClient
            settings.apply {
                @SuppressLint("SetJavaScriptEnabled")
                javaScriptEnabled = true

                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                userAgentString = "$userAgentString - stripe-android/${StripeSdkVersion.VERSION_NAME}"
            }

            setDownloadListener(StripeDownloadListener(webView.context))

            addJavascriptInterface(StripeJsInterface(), ANDROID_JS_INTERFACE)
            addJavascriptInterface(StripeJsInterfaceInternal(), ANDROID_JS_INTERNAL_INTERFACE)
        }
    }

    override fun initialize(
        embeddedComponentManager: EmbeddedComponentManager,
        listener: Listener?
    ) {
        if (this.controller != null) {
            throw IllegalStateException("EmbeddedComponentManager is already set")
        }
        this.controller = StripeConnectWebViewContainerController(
            view = this,
            embeddedComponentManager = embeddedComponentManager,
            embeddedComponent = embeddedComponent,
            listener = listener,
            listenerDelegate = listenerDelegate,
        )
        bindViewToController()
    }

    private fun bindViewToController() {
        val view = this.viewBinding?.root ?: return
        val controller = this.controller ?: return

        view.doOnAttach {
            controller.onViewAttached()
            val owner = view.findViewTreeLifecycleOwner()
            if (owner != null) {
                owner.lifecycle.addObserver(controller)
                owner.lifecycleScope.launch {
                    controller.stateFlow.collectLatest(::bindViewState)
                }
            }
        }

        view.doOnDetach {
            view.findViewTreeLifecycleOwner()?.lifecycle?.removeObserver(controller)
        }
    }

    override fun updateConnectInstance(appearance: Appearance) {
        val payload =
            ConnectInstanceJs(appearance = appearance.toJs())
        webView?.evaluateSdkJs(
            "updateConnectInstance",
            ConnectJson.encodeToJsonElement(payload).jsonObject
        )
    }

    override fun loadUrl(url: String) {
        webView?.clearCache(true)
        webView?.loadUrl(url)
    }

    private fun bindViewState(state: StripeConnectWebViewContainerState) {
        val viewBinding = this.viewBinding ?: return
        viewBinding.stripeWebView.setBackgroundColor(state.backgroundColor)
        viewBinding.stripeWebViewProgressBar.isVisible = state.isNativeLoadingIndicatorVisible
        if (state.isNativeLoadingIndicatorVisible) {
            viewBinding.stripeWebViewProgressBar.indeterminateTintList =
                ColorStateList.valueOf(state.nativeLoadingIndicatorColor)
        }
    }

    @VisibleForTesting
    internal inner class StripeConnectWebViewClient : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            initJavascriptBridge(view)
            controller?.onPageStarted()
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse
        ) {
            controller?.onReceivedError(request.url.toString(), errorResponse.statusCode, errorResponse.reasonPhrase)
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            // for some reason errorCode and description are only available in API 23+,
            // so we simply ignore the description for older devices
            val errorMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                error.description.toString()
            } else {
                null
            }
            controller?.onReceivedError(request.url.toString(), errorMessage = errorMessage)
        }

        private fun initJavascriptBridge(webView: WebView) {
            logger.debug("Initializing Javascript Bridge")
            webView.evaluateJavascript(
                """
                    $ANDROID_JS_INTERFACE.accountSessionClaimed = (message) => {
                        $ANDROID_JS_INTERNAL_INTERFACE.accountSessionClaimed(JSON.stringify(message));
                    };
                    $ANDROID_JS_INTERFACE.pageDidLoad = (message) => {
                        $ANDROID_JS_INTERNAL_INTERFACE.pageDidLoad(JSON.stringify(message));
                    };
                    $ANDROID_JS_INTERFACE.fetchInitParams = () => {
                        let params = $ANDROID_JS_INTERNAL_INTERFACE.fetchInitParams();
                        return Promise.resolve(JSON.parse(params));
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
                { resultString -> logger.debug("Javascript Bridge initialized. Result: \"$resultString\"") }
            )
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            return controller?.shouldOverrideUrlLoading(view.context, request) ?: false
        }
    }

    /**
     * A [WebChromeClient] that provides additional functionality for Stripe Connect Embedded Component WebViews.
     *
     * This class is currently empty, but it could be used to add additional functionality in the future
     * Setting a [WebChromeClient] (even an empty one) is necessary for certain functionality, like
     * [WebViewClient.shouldOverrideUrlLoading] to work properly.
     */
    inner class StripeWebChromeClient : WebChromeClient()

    private inner class StripeJsInterface {
        @JavascriptInterface
        fun debug(message: String) {
            logger.debug("Debug log from JS: $message")
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
        fun fetchInitParams(): String {
            val context = checkNotNull(webView?.context)
            val initialParams = checkNotNull(controller?.getInitialParams(context))
            logger.debug("InitParams fetched: ${initialParams.toDebugString()}")
            return ConnectJson.encodeToString(initialParams)
        }

        @JavascriptInterface
        fun onSetterFunctionCalled(message: String) {
            val parsed = ConnectJson.decodeFromString<SetterFunctionCalledMessage>(message)
            logger.debug("Setter function called: $parsed")

            controller?.onReceivedSetterFunctionCalled(parsed)
        }

        @JavascriptInterface
        fun openSecureWebView(message: String) {
            val secureWebViewData = ConnectJson.decodeFromString<SecureWebViewMessage>(message)
            logger.debug("Open secure web view with data: $secureWebViewData")
        }

        @JavascriptInterface
        fun pageDidLoad(message: String) {
            val pageLoadMessage = ConnectJson.decodeFromString<PageLoadMessage>(message)
            logger.debug("Page did load: $pageLoadMessage")

            controller?.onReceivedPageDidLoad()
        }

        @JavascriptInterface
        fun accountSessionClaimed(message: String) {
            val accountSessionClaimedMessage = ConnectJson.decodeFromString<AccountSessionClaimedMessage>(message)
            logger.debug("Account session claimed: $accountSessionClaimedMessage")
        }

        @JavascriptInterface
        fun fetchClientSecret(): String {
            return runBlocking {
                checkNotNull(controller?.fetchClientSecret())
            }
        }
    }

    private fun WebView.evaluateSdkJs(function: String, payload: JsonObject) {
        val command = "$ANDROID_JS_INTERFACE.$function($payload)"
        post {
            logger.debug("Evaluating JS: $command")
            evaluateJavascript(command, null)
        }
    }

    internal companion object {
        private const val ANDROID_JS_INTERFACE = "Android"
        private const val ANDROID_JS_INTERNAL_INTERFACE = "AndroidInternal"
    }
}
