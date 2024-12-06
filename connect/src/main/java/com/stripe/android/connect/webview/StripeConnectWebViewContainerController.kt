package com.stripe.android.connect.webview

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
import android.webkit.WebResourceRequest
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat.checkSelfPermission
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
import com.stripe.android.connect.webview.serialization.ConnectInstanceJs
import com.stripe.android.connect.webview.serialization.SetOnLoadError
import com.stripe.android.connect.webview.serialization.SetOnLoaderStart
import com.stripe.android.connect.webview.serialization.SetterFunctionCalledMessage
import com.stripe.android.core.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(PrivateBetaConnectSDK::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class StripeConnectWebViewContainerController<Listener : StripeEmbeddedComponentListener>(
    private val view: StripeConnectWebViewContainerInternal,
    private val embeddedComponentManager: EmbeddedComponentManager,
    private val embeddedComponent: StripeEmbeddedComponent,
    private val listener: Listener?,
    private val listenerDelegate: ComponentListenerDelegate<Listener>,
    private val stripeIntentLauncher: StripeIntentLauncher = StripeIntentLauncherImpl(),
    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG),
) : DefaultLifecycleObserver {

    private val _stateFlow = MutableStateFlow(StripeConnectWebViewContainerState())

    /**
     * Flow of the container state.
     */
    val stateFlow: StateFlow<StripeConnectWebViewContainerState>
        get() = _stateFlow.asStateFlow()

    private val inProgressRequests: MutableMap<PermissionRequest, Job> = mutableMapOf()

    /**
     * Callback to invoke when the view is attached.
     */
    fun onViewAttached() {
        view.loadUrl(embeddedComponentManager.getStripeURL(embeddedComponent))
    }

    /**
     * Callback to invoke when the page started loading.
     */
    fun onPageStarted() {
        updateState { copy(isNativeLoadingIndicatorVisible = !receivedSetOnLoaderStart) }
    }

    fun onReceivedError(requestUrl: String, httpStatusCode: Int? = null, errorMessage: String? = null) {
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
        listener?.onLoadError(RuntimeException(errorString)) // TODO - wrap error better
    }

    fun shouldOverrideUrlLoading(context: Context, request: WebResourceRequest): Boolean {
        val url = request.url
        return if (url.host?.lowercase() in ALLOWLISTED_HOSTS) {
            // TODO - add an analytic event here to track this unexpected behavior
            logger.warning("(StripeConnectWebViewClient) Received pop-up for allow-listed host: $url")
            false // Allow the request to propagate so we open URL in WebView, but this is not an expected operation
        } else if (
            url.scheme.equals("https", ignoreCase = true) || url.scheme.equals("http", ignoreCase = true)
        ) {
            // open the URL in an external browser for safety and to preserve back navigation
            logger.debug("(StripeConnectWebViewClient) Opening URL in external browser: $url")
            stripeIntentLauncher.launchSecureExternalWebTab(context, url)
            true // block the request since we're opening it in a secure external tab
        } else {
            logger.debug("(StripeConnectWebViewClient) Opening non-http/https pop-up request: $url")
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
                            if (stateFlow.value.receivedPageDidLoad) {
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

    fun getInitialParams(context: Context): ConnectInstanceJs {
        return embeddedComponentManager.getInitialParams(context)
    }

    /**
     *
     */
    suspend fun onPermissionRequest(context: Context, request: PermissionRequest) {
        // we only care about camera permissions (audio/video)
        val permissionsRequested = request.resources.filter {
            it in listOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE, PermissionRequest.RESOURCE_VIDEO_CAPTURE)
        }.toTypedArray()
        if (permissionsRequested.isEmpty()) {
            request.deny() // no supported permissions were requested, so reject the request
            return
        }

        if (checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            request.grant(permissionsRequested)
        } else {
            val isGranted = embeddedComponentManager.requestCameraPermission()
            withContext(Dispatchers.Main) {
                if (isGranted) {
                    request.grant(permissionsRequested)
                } else {
                    request.deny()
                }
            }
            inProgressRequests.remove(request)
        }
//        inProgressRequests[request] = job
    }

    /**
     *
     */
    fun onPermissionRequestCanceled(request: PermissionRequest) {
        inProgressRequests.remove(request)?.also { it.cancel() }
    }

    /**
     * Callback to invoke upon receiving 'pageDidLoad' message.
     */
    fun onReceivedPageDidLoad() {
        view.updateConnectInstance(embeddedComponentManager.appearanceFlow.value)
        updateState { copy(receivedPageDidLoad = true) }
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
