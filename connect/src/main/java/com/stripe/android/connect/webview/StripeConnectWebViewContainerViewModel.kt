package com.stripe.android.connect.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
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
import com.stripe.android.connect.util.Clock
import com.stripe.android.connect.webview.StripeConnectWebView.Delegate
import com.stripe.android.connect.webview.serialization.ConnectInstanceJs
import com.stripe.android.connect.webview.serialization.OpenFinancialConnectionsMessage
import com.stripe.android.connect.webview.serialization.SetOnLoaderStart
import com.stripe.android.connect.webview.serialization.SetterFunctionCalledMessage
import com.stripe.android.connect.webview.serialization.SetterFunctionCalledMessage.UnknownValue
import com.stripe.android.core.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

/**
 * ViewModel for [StripeConnectWebViewContainer]. Importantly, it also provides a cached WebView instance.
 * Creating and caching a View in a ViewModel is generally bad practice, but it's necessary in our case to provide
 * a good experience for both SDK integrators and end users.
 *
 * The issue is that the typical View state restoration mechanism does essentially nothing for WebViews. Any time
 * a WebView is recreated, it loses its state and reloads the page. This would be a bad UX especially for components
 * with forms.
 *
 * As of this writing, the official guidance is to "avoid activity recreation by specifying the configuration changes
 * handled by your app (rather than by the system)". However, that would likely be infeasible for users wanting to
 * integrate this SDK into their apps.
 *
 * So, the approach here is to:
 *  1. Create the WebView using the application context in the ViewModel.
 *  2. Cache the WebView in the ViewModel, which is retained across configuration changes.
 *  3. Directly handle WebView interactions in the ViewModel itself (as opposed to the WebView's containing view),
 *     otherwise WebView events may be dropped during View recreation.
 *  4. Whenever an activity [Context] is needed, find it within the view tree hierarchy.
 *
 * @see https://developer.android.com/develop/ui/compose/quick-guides/content/manage-webview-state
 */
@OptIn(PrivateBetaConnectSDK::class)
internal class StripeConnectWebViewContainerViewModel(
    private val application: Application,
    private val clock: Clock,
    private val embeddedComponentManager: EmbeddedComponentManager,
    private val embeddedComponent: StripeEmbeddedComponent,
    private val analyticsService: ComponentAnalyticsService,
    private val stripeIntentLauncher: StripeIntentLauncher = StripeIntentLauncherImpl(),
    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG),
    createWebView: CreateWebView = CreateWebView(::StripeConnectWebView),
) : ViewModel(),
    DefaultLifecycleObserver {

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
     * Flow of component events.
     */
    val eventFlow: Flow<ComponentEvent> get() = _eventFlow.asSharedFlow()

    @VisibleForTesting
    internal val delegate = StripeConnectWebViewDelegate()

    @SuppressLint("StaticFieldLeak") // Should be safe because we're using application.
    val webView: StripeConnectWebView =
        createWebView(
            application = application,
            delegate = delegate,
            logger = logger
        )

    /**
     * Callback to invoke when the view is attached.
     */
    fun onViewAttached() {
        updateState { copy(didBeginLoadingMillis = clock.millis()) }
        webView.loadUrl(embeddedComponentManager.getStripeURL(embeddedComponent))

        analyticsService.track(ConnectAnalyticsEvent.ComponentViewed(stateFlow.value.pageViewId))
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

    @VisibleForTesting
    @Suppress("TooManyFunctions")
    internal inner class StripeConnectWebViewDelegate : Delegate {
        override var propsJson: JsonObject?
            get() = this@StripeConnectWebViewContainerViewModel.propsJson
            set(value) {
                this@StripeConnectWebViewContainerViewModel.propsJson = value
            }

        override fun onPageStarted(url: String) {
            updateState { copy(isNativeLoadingIndicatorVisible = !receivedSetOnLoaderStart) }

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

        override fun onPageFinished(url: String) {
            val timeToLoad = clock.millis() - (stateFlow.value.didBeginLoadingMillis ?: 0)
            analyticsService.track(ConnectAnalyticsEvent.WebPageLoaded(timeToLoad))
        }

        override fun onReceivedError(
            requestUrl: String,
            httpStatusCode: Int?,
            errorMessage: String?,
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

        override fun onErrorDeserializingWebMessage(
            webFunctionName: String,
            error: Throwable,
        ) {
            analyticsService.track(
                ConnectAnalyticsEvent.WebErrorDeserializeMessage(
                    message = webFunctionName,
                    error = error.javaClass.simpleName,
                    pageViewId = stateFlow.value.pageViewId,
                )
            )
        }

        override fun onMerchantIdChanged(merchantId: String) {
            analyticsService.merchantId = merchantId
        }

        override fun shouldOverrideUrlLoading(context: Context, url: Uri): Boolean {
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
                url.scheme.equals("https", ignoreCase = true) ||
                url.scheme.equals("http", ignoreCase = true)
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

        override suspend fun fetchClientSecret(): String? {
            return embeddedComponentManager.fetchClientSecret()
        }

        override fun getInitialParams(context: Context): ConnectInstanceJs {
            return embeddedComponentManager.getInitialParams(context)
        }

        override suspend fun onPermissionRequest(activity: Activity, request: PermissionRequest) {
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
            val isGranted = embeddedComponentManager.requestCameraPermission(activity) ?: return
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

        override suspend fun onChooseFile(
            activity: Activity,
            filePathCallback: ValueCallback<Array<Uri>>,
            requestIntent: Intent
        ) {
            var result: Array<Uri>? = null
            try {
                result = embeddedComponentManager.chooseFile(activity, requestIntent)
            } finally {
                // Ensure `filePathCallback` always gets a value.
                filePathCallback.onReceiveValue(result)
            }
        }

        override suspend fun onOpenFinancialConnections(
            activity: Activity,
            message: OpenFinancialConnectionsMessage,
        ) {
            val result = embeddedComponentManager.presentFinancialConnections(
                activity = activity,
                clientSecret = message.clientSecret,
                connectedAccountId = message.connectedAccountId,
            )
            webView.setCollectMobileFinancialConnectionsResult(
                id = message.id,
                result = result,
            )
        }

        override fun onReceivedPageDidLoad(pageViewId: String) {
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

        override fun onReceivedSetterFunctionCalled(message: SetterFunctionCalledMessage) {
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
    }

    private inline fun updateState(
        update: StripeConnectWebViewContainerState.() -> StripeConnectWebViewContainerState
    ) {
        _stateFlow.value = update(stateFlow.value)
    }

    private fun Uri.sanitize(): String {
        return buildUpon().clearQuery().fragment(null).build().toString()
    }

    internal companion object {
        private val ALLOWLISTED_HOSTS = setOf("connect.stripe.com", "connect-js.stripe.com")

        val BASE_DEPENDENCIES_KEY = object : CreationExtras.Key<BaseDependencies> {}

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val baseDependencies = this[BASE_DEPENDENCIES_KEY] as BaseDependencies
                StripeConnectWebViewContainerViewModel(
                    application = this[APPLICATION_KEY] as Application,
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

internal fun interface CreateWebView {
    operator fun invoke(application: Application, delegate: Delegate, logger: Logger): StripeConnectWebView
}
