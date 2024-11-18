package com.stripe.android.connect.webview

import androidx.annotation.RestrictTo
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.StripeEmbeddedComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(PrivateBetaConnectSDK::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class StripeConnectWebViewContainerController(
    private val view: StripeConnectWebViewContainerInternal,
    private val embeddedComponentManager: EmbeddedComponentManager,
    private val embeddedComponent: StripeEmbeddedComponent,
) : DefaultLifecycleObserver {

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
        view.loadUrl(embeddedComponentManager.getStripeURL(embeddedComponent))
    }

    /**
     * Callback to invoke when the page started loading.
     */
    fun onPageStarted() {
        updateState { copy(isNativeLoadingIndicatorVisible = !receivedSetOnLoaderStart) }
    }

    override fun onCreate(owner: LifecycleOwner) {
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    // Bind appearance changes in the manager to the WebView (only when page is loaded).
                    embeddedComponentManager.appearanceFlow
                        .collectLatest { appearance ->
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

    /**
     * Callback to invoke upon receiving 'pageDidLoad' message.
     */
    fun onReceivedPageDidLoad() {
        view.updateConnectInstance(embeddedComponentManager.appearanceFlow.value)
        updateState { copy(receivedPageDidLoad = true) }
    }

    /**
     * Callback to invoke upon receiving 'setOnLoaderStart' message.
     */
    fun onReceivedSetOnLoaderStart() {
        updateState {
            copy(
                receivedSetOnLoaderStart = true,
                isNativeLoadingIndicatorVisible = false,
            )
        }
    }

    private inline fun updateState(
        update: StripeConnectWebViewContainerState.() -> StripeConnectWebViewContainerState
    ) {
        _stateFlow.value = update(stateFlow.value)
    }
}
