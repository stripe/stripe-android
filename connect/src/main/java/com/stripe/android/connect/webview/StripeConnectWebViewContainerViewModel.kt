package com.stripe.android.connect.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.connect.BuildConfig
import com.stripe.android.connect.ComponentEvent
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.StripeEmbeddedComponent
import com.stripe.android.connect.analytics.ComponentAnalyticsService
import com.stripe.android.connect.analytics.ConnectAnalyticsEvent
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.util.Clock
import com.stripe.android.connect.webview.serialization.AccountSessionClaimedMessage
import com.stripe.android.connect.webview.serialization.ConnectInstanceJs
import com.stripe.android.connect.webview.serialization.ConnectJson
import com.stripe.android.connect.webview.serialization.OpenFinancialConnectionsMessage
import com.stripe.android.connect.webview.serialization.PageLoadMessage
import com.stripe.android.connect.webview.serialization.SecureWebViewMessage
import com.stripe.android.connect.webview.serialization.SetCollectMobileFinancialConnectionsResultPayloadJs
import com.stripe.android.connect.webview.serialization.SetOnLoaderStart
import com.stripe.android.connect.webview.serialization.SetterFunctionCalledMessage
import com.stripe.android.connect.webview.serialization.SetterFunctionCalledMessage.UnknownValue
import com.stripe.android.connect.webview.serialization.toJs
import com.stripe.android.core.Logger
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@Suppress("TooManyFunctions")
@OptIn(PrivateBetaConnectSDK::class)
internal class StripeConnectWebViewContainerViewModel(
    private val applicationContext: Context,
    private val clock: Clock,
    private val embeddedComponentManager: EmbeddedComponentManager,
    private val embeddedComponent: StripeEmbeddedComponent,
    private val analyticsService: ComponentAnalyticsService,
    private val stripeIntentLauncher: StripeIntentLauncher = StripeIntentLauncherImpl(),
    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG),
) : ViewModel(), DefaultLifecycleObserver {

    init {
        analyticsService.track(ConnectAnalyticsEvent.ComponentCreated)
    }

    var propsJson: JsonObject? = null

    private val loggerTag = javaClass.simpleName
    private val _stateFlow = MutableStateFlow(StripeConnectWebViewContainerState())

    /**
     * Flow of the container state.
     */
    val stateFlow: StateFlow<StripeConnectWebViewContainerState>
        get() = _stateFlow.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ComponentEvent>(extraBufferCapacity = 1)

    /**
     * Flow of events.
     */
    val eventFlow: Flow<ComponentEvent> get() = _eventFlow.asSharedFlow()

    @VisibleForTesting
    internal val stripeWebViewClient = StripeConnectWebViewClient()

    @VisibleForTesting
    internal val stripeWebChromeClient = StripeConnectWebChromeClient()

    @SuppressLint("StaticFieldLeak")
    val webView: WebView = WebView(applicationContext)
        .apply { initializeWebView(this) }

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

    /**
     * Callback to invoke when the view is attached.
     */
    fun onViewAttached() {
        updateState { copy(didBeginLoadingMillis = clock.millis()) }
        webView.loadUrl(embeddedComponentManager.getStripeURL(embeddedComponent))

        analyticsService.track(ConnectAnalyticsEvent.ComponentViewed(stateFlow.value.pageViewId))
    }

    /**
     * Callback to invoke when the page started loading.
     */
    fun onPageStarted(url: String?) {
        updateState { copy(isNativeLoadingIndicatorVisible = !receivedSetOnLoaderStart) }

        if (url != null) {
            val pageLoadUrl = Uri.parse(url)
            val expectedUrl = Uri.parse(embeddedComponentManager.getStripeURL(embeddedComponent))
            if (
                pageLoadUrl.scheme != expectedUrl.scheme ||
                pageLoadUrl.host != expectedUrl.host ||
                pageLoadUrl.path != expectedUrl.path
            ) {
                // expected URL doesn't match what we navigated to
                analyticsService.track(ConnectAnalyticsEvent.WebErrorUnexpectedNavigation(pageLoadUrl.sanitize()))
            }
        }
    }

    /**
     * Callback to invoke when the page finished loading.
     */
    fun onPageFinished() {
        val timeToLoad = clock.millis() - (stateFlow.value.didBeginLoadingMillis ?: 0)
        analyticsService.track(ConnectAnalyticsEvent.WebPageLoaded(timeToLoad))
    }

    /**
     * Callback to invoke when the webview received a network error. If the error was an HTTP error,
     * [httpStatusCode] will be non-null.
     */
    fun onReceivedError(
        requestUrl: String,
        httpStatusCode: Int? = null,
        errorMessage: String? = null,
        isMainPageLoad: Boolean,
    ) {
        val errorString = buildString {
            if (httpStatusCode != null) {
                append("Received $httpStatusCode loading $requestUrl")
            } else {
                append("Received error loading $requestUrl")
            }
            if (errorMessage != null) {
                append(": $errorMessage")
            }
        }
        logger.debug("($loggerTag) $errorString")

        // don't send errors for requests that aren't for the main page load
        if (isMainPageLoad) {
            // TODO - wrap error better
            _eventFlow.tryEmit(ComponentEvent.LoadError(RuntimeException(errorString)))
            analyticsService.track(
                ConnectAnalyticsEvent.WebErrorPageLoad(
                    status = httpStatusCode,
                    error = errorMessage,
                    url = requestUrl
                )
            )
        }
    }

    fun onErrorDeserializingWebMessage(
        webFunctionName: String,
        error: Exception,
    ) {
        analyticsService.track(
            ConnectAnalyticsEvent.WebErrorDeserializeMessage(
                message = webFunctionName,
                error = error.javaClass.simpleName,
                pageViewId = stateFlow.value.pageViewId,
            )
        )
    }

    /**
     * Callback whenever the merchant ID changes, such as in the
     */
    fun onMerchantIdChanged(merchantId: String) {
        analyticsService.merchantId = merchantId
    }

    /**
     * Callback to invoke when the webview begins loading a URL. Returns false if the URL
     * should be loaded in the webview, true otherwise. Returning true has the effect of blocking
     * the webview's navigation to the URL.
     */
    fun shouldOverrideUrlLoading(context: Context, request: WebResourceRequest): Boolean {
        val url = request.url
        return if (url.host?.lowercase() in ALLOWLISTED_HOSTS) {
            val sanitizedUrl = url.sanitize()
            logger.warning("($loggerTag) Received pop-up for allow-listed host: $sanitizedUrl")
            analyticsService.track(
                ConnectAnalyticsEvent.ClientError(
                    errorCode = "unexpected_popup",
                    errorMessage = "Received pop-up for allow-listed host: $sanitizedUrl"
                )
            )
            false // Allow the request to propagate so we open URL in WebView, but this is not an expected operation
        } else if (
            url.scheme.equals("https", ignoreCase = true) || url.scheme.equals("http", ignoreCase = true)
        ) {
            // open the URL in an external browser for safety and to preserve back navigation
            logger.debug("($loggerTag) Opening URL in external browser: $url")
            stripeIntentLauncher.launchSecureExternalWebTab(context, url)
            true // block the request since we're opening it in a secure external tab
        } else {
            logger.debug("($loggerTag) Opening non-http/https pop-up request: $url")
            if (url.scheme.equals("mailto", ignoreCase = true)) {
                stripeIntentLauncher.launchEmailLink(context, url)
            } else {
                stripeIntentLauncher.launchUrlWithSystemHandler(context, url)
            }
            true // block the request since we're opening it via the system handler
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    // Bind appearance changes in the manager to the WebView (only when page is loaded).
                    embeddedComponentManager.appearanceFlow
                        .collectLatest { appearance ->
                            updateState { copy(appearance = appearance) }
                            if (stateFlow.value.pageViewId != null) {
                                webView.updateConnectInstance(appearance)
                            }
                        }
                }
            }
        }
    }

    /**
     * Fetch the client secret from the consumer of the SDK.
     */
    suspend fun fetchClientSecret(): String? {
        return embeddedComponentManager.fetchClientSecret()
    }

    /**
     * Get the initial parameters for the Connect SDK instance.
     */
    fun getInitialParams(context: Context): ConnectInstanceJs {
        return embeddedComponentManager.getInitialParams(context)
    }

    /**
     * Callback to invoke upon receiving a permission request from the webview.
     * Calls [PermissionRequest.grant] if the user grants permission to the resources
     * requested in [PermissionRequest.getResources], calls [PermissionRequest.deny] otherwise.
     *
     * An example of where this is called is when the webview requests access to the camera.
     */
    suspend fun onPermissionRequest(context: Context, request: PermissionRequest) {
        // we only care about camera permissions (audio/video)
        val permissionsRequested = request.resources.filter {
            it == PermissionRequest.RESOURCE_AUDIO_CAPTURE || it == PermissionRequest.RESOURCE_VIDEO_CAPTURE
        }.toTypedArray()
        if (permissionsRequested.isEmpty()) { // all calls to PermissionRequest must be on the main thread
            withContext(Dispatchers.Main) {
                request.deny() // no supported permissions were requested, so reject the request
                analyticsService.track(
                    ConnectAnalyticsEvent.ClientError(
                        errorCode = "unexpected_permissions_request",
                        errorMessage = "Unexpected permissions '${request.resources.joinToString(",")}' requested"
                    )
                )
                logger.warning(
                    "($loggerTag) Denying permission - '${request.resources.joinToString(",")}' are not supported"
                )
            }
            return
        }

        // all calls to PermissionRequest must be on the main thread
        val isGranted = embeddedComponentManager.requestCameraPermission(context) ?: return
        withContext(Dispatchers.Main) {
            if (isGranted) {
                logger.debug("($loggerTag) Granting permission - user accepted permission")
                request.grant(permissionsRequested)
            } else {
                logger.debug("($loggerTag) Denying permission - user denied permission")
                request.deny()
            }
        }
    }

    suspend fun onChooseFile(
        context: Context,
        filePathCallback: ValueCallback<Array<Uri>>,
        requestIntent: Intent
    ) {
        var result: Array<Uri>? = null
        try {
            result = embeddedComponentManager.chooseFile(context, requestIntent)
        } finally {
            // Ensure `filePathCallback` always gets a value.
            filePathCallback.onReceiveValue(result)
        }
    }

    suspend fun onOpenFinancialConnections(
        context: Context,
        message: OpenFinancialConnectionsMessage,
    ) {
        val result = embeddedComponentManager.presentFinancialConnections(
            context = context,
            clientSecret = message.clientSecret,
            connectedAccountId = message.connectedAccountId,
        )
        webView.setCollectMobileFinancialConnectionsResult(
            id = message.id,
            result = result,
        )
    }

    /**
     * Callback to invoke upon receiving 'pageDidLoad' message.
     */
    fun onReceivedPageDidLoad(pageViewId: String) {
        webView.updateConnectInstance(embeddedComponentManager.appearanceFlow.value)
        updateState { copy(pageViewId = pageViewId) }

        // right now view onAttach and begin load happen at the same time,
        // so timeToLoad and perceivedTimeToLoad are the same value
        val timeToLoad = clock.millis() - (stateFlow.value.didBeginLoadingMillis ?: 0)
        analyticsService.track(
            ConnectAnalyticsEvent.WebComponentLoaded(
                pageViewId = pageViewId,
                timeToLoadMs = timeToLoad,
                perceivedTimeToLoadMs = timeToLoad,
            )
        )
    }

    /**
     * Callback to invoke upon receiving 'onSetterFunctionCalled' message.
     */
    fun onReceivedSetterFunctionCalled(message: SetterFunctionCalledMessage) {
        when (message.value) {
            is SetOnLoaderStart -> {
                updateState {
                    copy(
                        receivedSetOnLoaderStart = true,
                        isNativeLoadingIndicatorVisible = false,
                    )
                }
            }
            is UnknownValue -> {
                analyticsService.track(
                    ConnectAnalyticsEvent.WebWarnUnrecognizedSetter(
                        setter = message.setter,
                        pageViewId = stateFlow.value.pageViewId
                    )
                )
            }
            else -> {
                // Fallthrough.
            }
        }
        _eventFlow.tryEmit(ComponentEvent.Message(message))
    }

    private inline fun updateState(
        update: StripeConnectWebViewContainerState.() -> StripeConnectWebViewContainerState
    ) {
        _stateFlow.value = update(stateFlow.value)
    }

    private fun Uri.sanitize(): String {
        return buildUpon().clearQuery().fragment(null).build().toString()
    }

    @VisibleForTesting
    internal inner class StripeConnectWebViewClient : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            this@StripeConnectWebViewContainerViewModel.onPageStarted(url)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            this@StripeConnectWebViewContainerViewModel.onPageFinished()
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse
        ) {
            this@StripeConnectWebViewContainerViewModel.onReceivedError(
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
            this@StripeConnectWebViewContainerViewModel.onReceivedError(
                requestUrl = request.url.toString(),
                errorMessage = errorMessage,
                isMainPageLoad = request.isForMainFrame
            )
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            return this@StripeConnectWebViewContainerViewModel.shouldOverrideUrlLoading(view.context, request)
        }
    }

    /**
     * A [WebChromeClient] that provides additional functionality for Stripe Connect Embedded Component WebViews.
     */
    internal inner class StripeConnectWebChromeClient : WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest) {
            webView.findViewTreeLifecycleOwner()?.lifecycleScope
                ?.launch {
                    this@StripeConnectWebViewContainerViewModel.onPermissionRequest(webView.context, request)
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

            lifecycleScope.launch {
                this@StripeConnectWebViewContainerViewModel.onChooseFile(
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
            logger.debug("(StripeConnectWebViewContainer) Debug log from JS: $message")
        }

        @JavascriptInterface
        fun fetchInitComponentProps(): String {
            logger.debug("(StripeConnectWebViewContainer) InitComponentProps fetched")
            return ConnectJson.encodeToString(propsJson ?: JsonObject(emptyMap()))
        }

        @JavascriptInterface
        fun log(message: String) {
            logger.debug("(StripeConnectWebViewContainer) Log from JS: $message")
        }

        @JavascriptInterface
        fun fetchInitParams(): String {
            val context = checkNotNull(webView.context)
            val initialParams = getInitialParams(context)
            logger.debug("(StripeConnectWebViewContainer) InitParams fetched: ${initialParams.toDebugString()}")
            return ConnectJson.encodeToString(initialParams)
        }

        @JavascriptInterface
        fun onSetterFunctionCalled(message: String) {
            val parsed = tryDeserializeWebMessage<SetterFunctionCalledMessage>(
                webFunctionName = "onSetterFunctionCalled",
                message = message,
            ) ?: return
            logger.debug("(StripeConnectWebViewContainer) Setter function called: $parsed")

            this@StripeConnectWebViewContainerViewModel.onReceivedSetterFunctionCalled(parsed)
        }

        @JavascriptInterface
        fun openSecureWebView(message: String) {
            val secureWebViewData = tryDeserializeWebMessage<SecureWebViewMessage>(
                webFunctionName = "openSecureWebView",
                message = message,
            )
            logger.debug("(StripeConnectWebViewContainer) Open secure web view with data: $secureWebViewData")
        }

        @JavascriptInterface
        fun pageDidLoad(message: String) {
            val pageLoadMessage = tryDeserializeWebMessage<PageLoadMessage>(
                webFunctionName = "pageDidLoad",
                message = message,
            ) ?: return
            logger.debug("(StripeConnectWebViewContainer) Page did load: $pageLoadMessage")

            this@StripeConnectWebViewContainerViewModel.onReceivedPageDidLoad(pageLoadMessage.pageViewId)
        }

        @JavascriptInterface
        fun accountSessionClaimed(message: String) {
            val accountSessionClaimedMessage = tryDeserializeWebMessage<AccountSessionClaimedMessage>(
                webFunctionName = "accountSessionClaimed",
                message = message,
            ) ?: return
            logger.debug("(StripeConnectWebViewContainer) Account session claimed: $accountSessionClaimedMessage")

            this@StripeConnectWebViewContainerViewModel.onMerchantIdChanged(accountSessionClaimedMessage.merchantId)
        }

        @JavascriptInterface
        fun openFinancialConnections(message: String) {
            val parsed = ConnectJson.decodeFromString<OpenFinancialConnectionsMessage>(message)
            logger.debug("(StripeConnectWebViewContainer) Open FinancialConnections: $parsed")

            val lifecycleScope = webView.findViewTreeLifecycleOwner()?.lifecycleScope
                ?: return
            lifecycleScope.launch {
                this@StripeConnectWebViewContainerViewModel.onOpenFinancialConnections(
                    context = webView.context,
                    message = parsed,
                )
            }
        }

        @JavascriptInterface
        fun fetchClientSecret(): String {
            return runBlocking {
                checkNotNull(this@StripeConnectWebViewContainerViewModel.fetchClientSecret())
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
            onErrorDeserializingWebMessage(
                webFunctionName = webFunctionName,
                error = e,
            )
            null
        }
    }

    private fun WebView.updateConnectInstance(appearance: Appearance) {
        val payload =
            ConnectInstanceJs(appearance = appearance.toJs())
        evaluateSdkJs(
            "updateConnectInstance",
            ConnectJson.encodeToJsonElement(payload).jsonObject
        )
    }

    private fun WebView.setCollectMobileFinancialConnectionsResult(
        id: String,
        result: FinancialConnectionsSheetResult?
    ) {
        val payload = SetCollectMobileFinancialConnectionsResultPayloadJs.from(id, result)
        callSetterWithSerializableValue(
            setter = "setCollectMobileFinancialConnectionsResult",
            value = ConnectJson.encodeToJsonElement(payload).jsonObject
        )
    }

    private fun WebView.callSetterWithSerializableValue(setter: String, value: JsonElement) {
        evaluateSdkJs(
            "callSetterWithSerializableValue",
            buildJsonObject {
                put("setter", setter)
                put("value", value)
            }
        )
    }

    private fun WebView.evaluateSdkJs(function: String, payload: JsonObject) {
        val command = "${ANDROID_JS_INTERFACE}.$function($payload)"
        post {
            logger.debug("(StripeConnectWebViewContainer) Evaluating JS: $command")
            evaluateJavascript(command, null)
        }
    }

    internal companion object {
        private val ALLOWLISTED_HOSTS = setOf("connect.stripe.com", "connect-js.stripe.com")

        private const val ANDROID_JS_INTERFACE = "Android"

        val BASE_DEPENDENCIES_KEY = object : CreationExtras.Key<BaseDependencies> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val baseDependencies = this[BASE_DEPENDENCIES_KEY] as BaseDependencies
                StripeConnectWebViewContainerViewModel(
                    applicationContext = this[APPLICATION_KEY] as Context,
                    analyticsService = baseDependencies.analyticsService,
                    clock = baseDependencies.clock,
                    embeddedComponentManager = baseDependencies.embeddedComponentManager,
                    embeddedComponent = baseDependencies.embeddedComponent,
                    stripeIntentLauncher = baseDependencies.stripeIntentLauncher,
                    logger = baseDependencies.logger,
                )
            }
        }
    }

    internal data class BaseDependencies(
        val clock: Clock,
        val embeddedComponentManager: EmbeddedComponentManager,
        val embeddedComponent: StripeEmbeddedComponent,
        val stripeIntentLauncher: StripeIntentLauncher = StripeIntentLauncherImpl(),
        val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG),
    ) {
        val analyticsService = embeddedComponentManager.getComponentAnalyticsService(embeddedComponent)
    }
}
