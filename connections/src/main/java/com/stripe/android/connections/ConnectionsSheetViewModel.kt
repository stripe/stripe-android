package com.stripe.android.connections

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.stripe.android.connections.ConnectionsSheetViewEffect.FinishWithResult
import com.stripe.android.connections.ConnectionsSheetViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.connections.analytics.ConnectionsEventReporter
import com.stripe.android.connections.di.APPLICATION_ID
import com.stripe.android.connections.di.DaggerConnectionsSheetComponent
import com.stripe.android.connections.domain.FetchLinkAccountSession
import com.stripe.android.connections.domain.GenerateLinkAccountSessionManifest
import com.stripe.android.connections.model.LinkAccountSession
import com.stripe.android.connections.model.LinkAccountSessionManifest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

internal class ConnectionsSheetViewModel @Inject constructor(
    @Named(APPLICATION_ID) private val applicationId: String,
    private val starterArgs: ConnectionsSheetContract.Args,
    private val generateLinkAccountSessionManifest: GenerateLinkAccountSessionManifest,
    private val fetchLinkAccountSession: FetchLinkAccountSession,
    private val eventReporter: ConnectionsEventReporter
) : ViewModel() {

    private val _state = MutableStateFlow(ConnectionsSheetState())
    internal val state: StateFlow<ConnectionsSheetState> = _state
    private val _viewEffect = MutableSharedFlow<ConnectionsSheetViewEffect>()
    internal val viewEffect: SharedFlow<ConnectionsSheetViewEffect> = _viewEffect

    init {
        eventReporter.onPresented(starterArgs.configuration)
        fetchManifest()
    }

    /**
     * Fetches the [LinkAccountSessionManifest] from the Stripe API to get the hosted auth flow URL
     * as well as the success and cancel callback URLs to verify.
     */
    private fun fetchManifest() {
        viewModelScope.launch {
            kotlin.runCatching {
                generateLinkAccountSessionManifest(
                    clientSecret = starterArgs.configuration.linkAccountSessionClientSecret,
                    applicationId = applicationId
                )
            }.onFailure {
                onFatal(it)
            }.onSuccess {
                openAuthFlow(it)
            }
        }
    }

    /**
     * Builds the ChromeCustomTab intent to launch the hosted auth flow and launches it.
     *
     * @param manifest the manifest containing the hosted auth flow URL to launch
     *
     */
    private suspend fun openAuthFlow(manifest: LinkAccountSessionManifest) {
        // stores manifest in state for future references.
        _state.emit(
            state.value.copy(
                manifest = manifest,
                authFlowActive = true
            )
        )
        _viewEffect.emit(OpenAuthFlowWithUrl(manifest.hostedAuthUrl))
    }

    // If activity resumes and we did not receive a callback from the custom tabs,
    // then the user hit the back button or closed the custom tabs UI, so return result as
    // canceled.
    internal fun onResume() {
        if (_state.value.authFlowActive) {
            viewModelScope.launch {
                _viewEffect.emit(FinishWithResult(ConnectionsSheetResult.Canceled))
            }
        }
    }

    /**
     * On successfully completing the hosted auth flow and receiving the success callback intent,
     * fetch the updated [LinkAccountSession] model from the API and return that via the
     * [ConnectionsSheetResult] and [ConnectionsSheetResultCallback]
     */
    private fun fetchLinkAccountSession() {
        viewModelScope.launch {
            kotlin.runCatching {
                fetchLinkAccountSession(starterArgs.configuration.linkAccountSessionClientSecret)
            }.onSuccess {
                val result = ConnectionsSheetResult.Completed(it)
                eventReporter.onResult(starterArgs.configuration, result)
                _viewEffect.emit(FinishWithResult(result))
            }.onFailure {
                onFatal(it)
            }
        }
    }

    /**
     * If an error occurs during the connections sheet auth flow, return that error via the
     * [ConnectionsSheetResultCallback] and [ConnectionsSheetResult.Failed].
     *
     * @param throwable the error encountered during the connections sheet auth flow
     */
    private suspend fun onFatal(throwable: Throwable) {
        val result = ConnectionsSheetResult.Failed(throwable)
        eventReporter.onResult(starterArgs.configuration, result)
        _viewEffect.emit(FinishWithResult(result))
    }

    /**
     * If a user cancels the hosted auth flow either by closing the custom tab with the back button
     * or clicking a cancel link within the hosted auth flow and the activity received the canceled
     * URL callback, notify the [ConnectionsSheetResultCallback] of [ConnectionsSheetResult.Canceled]
     */
    private suspend fun onUserCancel() {
        val result = ConnectionsSheetResult.Canceled
        eventReporter.onResult(starterArgs.configuration, result)
        _viewEffect.emit(FinishWithResult(result))
    }

    /**
     * The hosted auth flow will redirect to a URL scheme stripe-auth://link-accounts which will be
     * handled by the [ConnectionsSheetActivity] per the intent filter in the Android manifest and
     * with the launch mode for the activity being `singleTask` it will trigger a new intent for the
     * activity which this method will receive
     *
     * @param intent the new intent with the redirect URL in the intent data
     */
    internal fun handleOnNewIntent(intent: Intent?) {
        updateState { copy(authFlowActive = false) }
        viewModelScope.launch {
            val manifest = _state.value.manifest
            when (intent?.data.toString()) {
                manifest?.successUrl -> fetchLinkAccountSession()
                manifest?.cancelUrl -> onUserCancel()
                else -> onFatal(Exception("Error processing ConnectionsSheet intent"))
            }
        }
    }

    private fun updateState(block: ConnectionsSheetState.() -> ConnectionsSheetState) {
        viewModelScope.launch {
            _state.emit(block(_state.value))
        }
    }

    class Factory(
        private val applicationSupplier: () -> Application,
        private val starterArgsSupplier: () -> ConnectionsSheetContract.Args,
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle? = null
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(
            key: String,
            modelClass: Class<T>,
            savedStateHandle: SavedStateHandle
        ): T {
            return DaggerConnectionsSheetComponent
                .builder()
                .application(applicationSupplier())
                .configuration(starterArgsSupplier())
                .build().viewModel as T
        }
    }

    internal companion object {
        internal const val MAX_ACCOUNTS = 100
    }
}
