package com.stripe.android.connect.webview

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.connect.BuildConfig
import com.stripe.android.connect.ComponentListenerDelegate
import com.stripe.android.connect.ComponentProps
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.StripeEmbeddedComponent
import com.stripe.android.connect.StripeEmbeddedComponentListener
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.toJsonObject
import com.stripe.android.connect.util.AndroidClock
import com.stripe.android.connect.webview.serialization.AccountSessionClaimedMessage
import com.stripe.android.connect.webview.serialization.ConnectInstanceJs
import com.stripe.android.connect.webview.serialization.ConnectJson
import com.stripe.android.connect.webview.serialization.OpenFinancialConnectionsMessage
import com.stripe.android.connect.webview.serialization.PageLoadMessage
import com.stripe.android.connect.webview.serialization.SecureWebViewMessage
import com.stripe.android.connect.webview.serialization.SetCollectMobileFinancialConnectionsResultPayloadJs
import com.stripe.android.connect.webview.serialization.SetterFunctionCalledMessage
import com.stripe.android.connect.webview.serialization.toJs
import com.stripe.android.core.Logger
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface StripeConnectWebViewContainer<Listener, Props>
    where Props : ComponentProps,
          Listener : StripeEmbeddedComponentListener {

    /**
     * Initializes the view. Must be called exactly once if and only if this view was created
     * through XML layout inflation.
     */
    fun initialize(
        embeddedComponentManager: EmbeddedComponentManager,
        listener: Listener?,
        props: Props,
    )
}

@OptIn(PrivateBetaConnectSDK::class)
internal interface StripeConnectWebViewContainerInternal {
    /**
     * Load the given URL in the WebView.
     */
    fun loadUrl(url: String)

    /**
     * Update the appearance of the Connect instance.
     */
    fun updateConnectInstance(appearance: Appearance)

    fun setCollectMobileFinancialConnectionsResult(id: String, result: FinancialConnectionsSheetResult?)
}

@OptIn(PrivateBetaConnectSDK::class)
internal class StripeConnectWebViewContainerImpl<Listener, Props>(
    val embeddedComponent: StripeEmbeddedComponent,
    embeddedComponentManager: EmbeddedComponentManager?,
    listener: Listener?,
    props: Props?,
    private val listenerDelegate: ComponentListenerDelegate<Listener>,
    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG),
) : StripeConnectWebViewContainer<Listener, Props>, StripeConnectWebViewContainerInternal
    where Props : ComponentProps,
          Listener : StripeEmbeddedComponentListener {

    private var containerView: FrameLayout? = null
    private var webView: WebView? = null
    private var progressBar: ProgressBar? = null

    @VisibleForTesting
    internal val stripeWebViewClient = StripeConnectWebViewClient()

    @VisibleForTesting
    internal val stripeWebChromeClient = StripeConnectWebChromeClient()

    /* Notes on initialization
     * -----------------------
     * An embedded component view can be instantiated in two ways:
     *  1. Calling one of the create methods in EmbeddedComponentManager
     *  2. XML layout inflation
     * In both cases, we need to initialize the view with the manager, listener, and props exactly
     * once.
     *
     * For (1), this is trivial since we can require the dependencies in the function signature
     * and initialize immediately. The user doesn't need to worry about initialization. In this
     * case, `embeddedComponentManager`, `props`, and `listener` from the constructor are all that
     * we use.
     *
     * For (2), we require the user to call initialize() after inflation. The values of the
     * constructor params will all be null, and we will only use the values passed to
     * `initialize()`. The one exception is `props`, which will first be set by internal function
     * `setPropsFromXml()` after which the user may merge in more props through `initialize()`
     * (if the user doesn't want to use the XML props, they shouldn't be specifying them).
     */

    private var controller: StripeConnectWebViewContainerController<Listener>? = null
    private var propsJson: JsonObject? = null

    init {
        if (embeddedComponentManager != null) {
            initializeInternal(
                embeddedComponentManager = embeddedComponentManager,
                listener = listener,
                propsJson = props?.toJsonObject()
            )
        }
    }

    internal fun initializeView(view: FrameLayout) {
        val context = view.context
        val applicationContext = context.applicationContext

        this.containerView = view

        val webView = WebView(applicationContext)
            .apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        this.webView = webView
        view.addView(webView)

        val progressBar = ProgressBar(context)
            .apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                )
            }
        this.progressBar = progressBar
        view.addView(progressBar)

        initializeWebView(webView)
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
        }
    }

    override fun initialize(
        embeddedComponentManager: EmbeddedComponentManager,
        listener: Listener?,
        props: Props,
    ) {
        initializeInternal(
            embeddedComponentManager = embeddedComponentManager,
            listener = listener,
            propsJson = props.toJsonObject()
        )
    }

    private fun initializeInternal(
        embeddedComponentManager: EmbeddedComponentManager,
        listener: Listener?,
        propsJson: JsonObject?,
    ) {
        if (this.controller != null) {
            throw IllegalStateException("Already initialized")
        }
        val oldProps = this.propsJson
        this.propsJson =
            when {
                propsJson == null -> oldProps
                oldProps == null -> propsJson
                else -> {
                    buildJsonObject {
                        (oldProps.entries + propsJson.entries).forEach { (k, v) ->
                            put(k, v)
                        }
                    }
                }
            }
        val analyticsService = embeddedComponentManager.getComponentAnalyticsService(embeddedComponent)
        this.controller = StripeConnectWebViewContainerController(
            view = this,
            analyticsService = analyticsService,
            clock = AndroidClock(),
            embeddedComponentManager = embeddedComponentManager,
            embeddedComponent = embeddedComponent,
            listener = listener,
            listenerDelegate = listenerDelegate,
        )
        bindViewToController()
    }

    private fun bindViewToController() {
        val view = this.containerView ?: return
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

    internal fun setPropsFromXml(props: Props) {
        // Only set props if uninitialized.
        if (controller == null) {
            this.propsJson = props.toJsonObject()
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

    override fun setCollectMobileFinancialConnectionsResult(
        id: String,
        result: FinancialConnectionsSheetResult?
    ) {
        val payload = SetCollectMobileFinancialConnectionsResultPayloadJs.from(id, result)
        callSetterWithSerializableValue(
            setter = "setCollectMobileFinancialConnectionsResult",
            value = ConnectJson.encodeToJsonElement(payload).jsonObject
        )
    }

    private fun callSetterWithSerializableValue(setter: String, value: JsonElement) {
        webView?.evaluateSdkJs(
            "callSetterWithSerializableValue",
            buildJsonObject {
                put("setter", setter)
                put("value", value)
            }
        )
    }

    override fun loadUrl(url: String) {
        webView?.loadUrl(url)
    }

    private fun bindViewState(state: StripeConnectWebViewContainerState) {
        val containerView = this.containerView ?: return
        val webView = this.webView ?: return
        val progressBar = this.progressBar ?: return

        logger.debug("Binding view state: $state")
        containerView.setBackgroundColor(state.backgroundColor)
        webView.setBackgroundColor(state.backgroundColor)
        progressBar.isVisible = state.isNativeLoadingIndicatorVisible
        webView.isVisible = !state.isNativeLoadingIndicatorVisible
        if (state.isNativeLoadingIndicatorVisible) {
            progressBar.indeterminateTintList = ColorStateList.valueOf(state.nativeLoadingIndicatorColor)
        }
    }

    @VisibleForTesting
    internal inner class StripeConnectWebViewClient : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            controller?.onPageStarted(url)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            controller?.onPageFinished()
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse
        ) {
            controller?.onReceivedError(
                requestUrl = request.url.toString(),
                httpStatusCode = errorResponse.statusCode,
                errorMessage = errorResponse.reasonPhrase,
                isMainPageLoad = request.isForMainFrame
            )
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            // for some reason errorCode and description are only available in API 23+,
            // so we simply ignore the description for older devices
            val errorMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                error.description.toString()
            } else {
                null
            }
            controller?.onReceivedError(
                requestUrl = request.url.toString(),
                errorMessage = errorMessage,
                isMainPageLoad = request.isForMainFrame
            )
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            return controller?.shouldOverrideUrlLoading(view.context, request) ?: false
        }
    }

    /**
     * A [WebChromeClient] that provides additional functionality for Stripe Connect Embedded Component WebViews.
     */
    internal inner class StripeConnectWebChromeClient : WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest) {
            val view = webView ?: return request.deny()

            view.findViewTreeLifecycleOwner()?.lifecycleScope
                ?.launch {
                    controller?.onPermissionRequest(view.context, request)
                }
                ?: return request.deny()
        }

        override fun onPermissionRequestCanceled(request: PermissionRequest) {
            super.onPermissionRequestCanceled(request)

            // currently a no-op since we don't hold any state from the permission
            // request and delegate all the UI to the Android system, meaning
            // there's no way for us to cancel any permissions UI
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            val lifecycleScope = webView.findViewTreeLifecycleOwner()?.lifecycleScope
                ?: return false
            val controller = this@StripeConnectWebViewContainerImpl.controller
                ?: return false
            lifecycleScope.launch {
                controller.onChooseFile(
                    context = webView.context,
                    filePathCallback = filePathCallback,
                    requestIntent = fileChooserParams.createIntent()
                )
            }
            return true
        }
    }

    private inner class StripeJsInterface {
        @JavascriptInterface
        fun debug(message: String) {
            logger.debug("Debug log from JS: $message")
        }

        @JavascriptInterface
        fun fetchInitComponentProps(): String {
            logger.debug("InitComponentProps fetched")
            return ConnectJson.encodeToString(propsJson ?: JsonObject(emptyMap()))
        }

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
            val parsed = tryDeserializeWebMessage<SetterFunctionCalledMessage>(
                webFunctionName = "onSetterFunctionCalled",
                message = message,
            ) ?: return
            logger.debug("Setter function called: $parsed")

            controller?.onReceivedSetterFunctionCalled(parsed)
        }

        @JavascriptInterface
        fun openSecureWebView(message: String) {
            val secureWebViewData = tryDeserializeWebMessage<SecureWebViewMessage>(
                webFunctionName = "openSecureWebView",
                message = message,
            )
            logger.debug("Open secure web view with data: $secureWebViewData")
        }

        @JavascriptInterface
        fun pageDidLoad(message: String) {
            val pageLoadMessage = tryDeserializeWebMessage<PageLoadMessage>(
                webFunctionName = "pageDidLoad",
                message = message,
            ) ?: return
            logger.debug("Page did load: $pageLoadMessage")

            controller?.onReceivedPageDidLoad(pageLoadMessage.pageViewId)
        }

        @JavascriptInterface
        fun accountSessionClaimed(message: String) {
            val accountSessionClaimedMessage = tryDeserializeWebMessage<AccountSessionClaimedMessage>(
                webFunctionName = "accountSessionClaimed",
                message = message,
            ) ?: return
            logger.debug("Account session claimed: $accountSessionClaimedMessage")

            controller?.onMerchantIdChanged(accountSessionClaimedMessage.merchantId)
        }

        @JavascriptInterface
        fun openFinancialConnections(message: String) {
            val parsed = ConnectJson.decodeFromString<OpenFinancialConnectionsMessage>(message)
            logger.debug("Open FinancialConnections: $parsed")

            val webView = this@StripeConnectWebViewContainerImpl.webView
                ?: return
            val lifecycleScope = webView.findViewTreeLifecycleOwner()?.lifecycleScope
                ?: return
            lifecycleScope.launch {
                controller?.onOpenFinancialConnections(
                    context = webView.context,
                    message = parsed,
                )
            }
        }

        @JavascriptInterface
        fun fetchClientSecret(): String {
            return runBlocking {
                checkNotNull(controller?.fetchClientSecret())
            }
        }
    }

    private inline fun <reified T> tryDeserializeWebMessage(
        webFunctionName: String,
        message: String,
    ): T? {
        return try {
            ConnectJson.decodeFromString<T>(message)
        } catch (e: IllegalArgumentException) {
            controller?.onErrorDeserializingWebMessage(
                webFunctionName = webFunctionName,
                error = e,
            )
            null
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
    }
}
