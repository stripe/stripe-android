package com.stripe.android.connect.webview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebResourceRequest
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.connect.BuildConfig
import com.stripe.android.connect.ComponentListenerDelegate
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.StripeEmbeddedComponent
import com.stripe.android.connect.StripeEmbeddedComponentListener
import com.stripe.android.connect.analytics.ComponentAnalyticsService
import com.stripe.android.connect.analytics.ConnectAnalyticsEvent
import com.stripe.android.connect.util.Clock
import com.stripe.android.connect.webview.serialization.ConnectInstanceJs
import com.stripe.android.connect.webview.serialization.SetOnLoadError
import com.stripe.android.connect.webview.serialization.SetOnLoaderStart
import com.stripe.android.connect.webview.serialization.SetterFunctionCalledMessage
import com.stripe.android.connect.webview.serialization.SetterFunctionCalledMessage.UnknownValue
import com.stripe.android.core.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("TooManyFunctions")
@OptIn(PrivateBetaConnectSDK::class)
internal class StripeConnectWebViewContainerController<Listener : StripeEmbeddedComponentListener>(
    private val view: StripeConnectWebViewContainerInternal,
    private val analyticsService: ComponentAnalyticsService,
    private val clock: Clock,
    private val embeddedComponentManager: EmbeddedComponentManager,
    private val embeddedComponent: StripeEmbeddedComponent,
    private val listener: Listener?,
    private val listenerDelegate: ComponentListenerDelegate<Listener>,
    private val stripeIntentLauncher: StripeIntentLauncher = StripeIntentLauncherImpl(),
    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG),
) : DefaultLifecycleObserver {

    init {
        analyticsService.track(ConnectAnalyticsEvent.ComponentCreated)
    }

    private val loggerTag = javaClass.simpleName
    private val _stateFlow = MutableStateFlow(StripeConnectWebViewContainerState())

    /**
     * Flow of the container state.
     */
    val stateFlow: StateFlow<StripeConnectWebViewContainerState>
        get() = _stateFlow.asStateFlow()

    /**
     * Callback to invoke when the view is attached.
     */
    fun onViewAttached() {
        updateState { copy(didBeginLoadingMillis = clock.millis()) }
        view.loadUrl(embeddedComponentManager.getStripeURL(embeddedComponent))

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
                val sanitizedUrl = pageLoadUrl.buildUpon().clearQuery().fragment(null).build().toString()
                analyticsService.track(ConnectAnalyticsEvent.WebErrorUnexpectedNavigation(sanitizedUrl))
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
            listener?.onLoadError(RuntimeException(errorString)) // TODO - wrap error better
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
        webMessage: String,
        error: String,
        errorMessage: String?,
    ) {
        analyticsService.track(
            ConnectAnalyticsEvent.WebErrorDeserializeMessage(
                message = webMessage,
                error = error,
                errorDescription = errorMessage,
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
            logger.warning("($loggerTag) Received pop-up for allow-listed host: $url")
            analyticsService.track(
                ConnectAnalyticsEvent.ClientError(
                    error = "Unexpected pop-up",
                    errorMessage = "Received pop-up for allow-listed host: $url"
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
                                view.updateConnectInstance(appearance)
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
                        error = "Unexpected permissions request",
                        errorMessage = "Unexpected permissions '${request.resources.joinToString()}' requested"
                    )
                )
                logger.warning(
                    "($loggerTag) Denying permission - '${request.resources.joinToString()}' are not supported"
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

    /**
     * Callback to invoke upon receiving 'pageDidLoad' message.
     */
    fun onReceivedPageDidLoad(pageViewId: String) {
        view.updateConnectInstance(embeddedComponentManager.appearanceFlow.value)
        updateState { copy(pageViewId = pageViewId) }

        // right now view onAttach and begin load happen at the same time,
        // so timeToLoad and perceivedTimeToLoad are the same value
        val timeToLoad = clock.millis() - (stateFlow.value.didBeginLoadingMillis ?: 0)
        analyticsService.track(
            ConnectAnalyticsEvent.WebComponentLoaded(
                pageViewId = pageViewId,
                timeToLoad = timeToLoad,
                perceivedTimeToLoad = timeToLoad,
            )
        )
    }

    /**
     * Callback to invoke upon receiving 'onSetterFunctionCalled' message.
     */
    fun onReceivedSetterFunctionCalled(message: SetterFunctionCalledMessage) {
        when (val value = message.value) {
            is SetOnLoaderStart -> {
                updateState {
                    copy(
                        receivedSetOnLoaderStart = true,
                        isNativeLoadingIndicatorVisible = false,
                    )
                }
                listener?.onLoaderStart()
            }
            is SetOnLoadError -> {
                // TODO - wrap error better
                listener?.onLoadError(RuntimeException("${value.error.type}: ${value.error.message}"))
            }
            else -> {
                if (value is UnknownValue) {
                    analyticsService.track(
                        ConnectAnalyticsEvent.WebWarnUnrecognizedSetter(
                            setter = message.setter,
                            pageViewId = stateFlow.value.pageViewId
                        )
                    )
                }
                with(listenerDelegate) {
                    listener?.delegate(message)
                }
            }
        }
    }

    private inline fun updateState(
        update: StripeConnectWebViewContainerState.() -> StripeConnectWebViewContainerState
    ) {
        _stateFlow.value = update(stateFlow.value)
    }

    internal companion object {
        private val ALLOWLISTED_HOSTS = setOf("connect.stripe.com", "connect-js.stripe.com")
    }
}
