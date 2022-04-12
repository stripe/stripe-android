package com.stripe.android.connections.presentation

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.stripe.android.connections.launcher.ConnectionsSheetContract.Args
import com.stripe.android.connections.launcher.ConnectionsSheetContract.Result
import com.stripe.android.connections.presentation.ConnectionsSheetViewEffect.FinishWithResult
import com.stripe.android.connections.presentation.ConnectionsSheetViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.connections.analytics.ConnectionsEventReporter
import com.stripe.android.connections.di.APPLICATION_ID
import com.stripe.android.connections.di.DaggerConnectionsSheetComponent
import com.stripe.android.connections.domain.FetchLinkAccountSession
import com.stripe.android.connections.domain.FetchLinkAccountSessionForToken
import com.stripe.android.connections.domain.GenerateLinkAccountSessionManifest
import com.stripe.android.connections.model.LinkAccountSession
import com.stripe.android.connections.model.LinkAccountSessionManifest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

internal class ConnectionsSheetViewModel @Inject constructor(
    @Named(APPLICATION_ID) private val applicationId: String,
    private val starterArgs: Args,
    private val generateLinkAccountSessionManifest: GenerateLinkAccountSessionManifest,
    private val fetchLinkAccountSession: FetchLinkAccountSession,
    private val fetchLinkAccountSessionForToken: FetchLinkAccountSessionForToken,
    private val savedStateHandle: SavedStateHandle,
    private val eventReporter: ConnectionsEventReporter
) : ViewModel() {

    // on process recreation - restore saved fields from [SavedStateHandle].
    private val _state = MutableStateFlow(ConnectionsSheetState().from(savedStateHandle))
    internal val state: StateFlow<ConnectionsSheetState> = _state

    private val _viewEffect = MutableSharedFlow<ConnectionsSheetViewEffect>()
    internal val viewEffect: SharedFlow<ConnectionsSheetViewEffect> = _viewEffect

    init {
        eventReporter.onPresented(starterArgs.configuration)
        // avoid re-fetching manifest if already exists (this will happen on process recreations)
        if (state.value.manifest == null) {
            fetchManifest()
        }
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
        _state.updateAndPersist {
            it.copy(
                manifest = manifest,
                authFlowActive = true
            )
        }
        _viewEffect.emit(OpenAuthFlowWithUrl(manifest.hostedAuthUrl))
    }

    /**
     * Activity recreation changes the lifecycle order:
     *
     * - If config change happens while in web flow: onResume -> onNewIntent -> activityResult
     * - If no config change happens: onActivityResult -> onNewIntent -> onResume
     *
     * (note [handleOnNewIntent] will just get called if user completed the web flow and clicked
     * the deeplink that redirects back to the app)
     *
     * We need to rely on a post-onNewIntent lifecycle callback to figure if the user completed
     * or cancelled the web flow. [ConnectionsSheetState.activityRecreated] will be used to
     * figure which lifecycle callback happens after onNewIntent.
     *
     * @see onResume (we rely on this on regular flows)
     * @see onActivityResult (we rely on this on config changes)
     */
    internal fun onActivityRecreated() {
        _state.updateAndPersist { it.copy(activityRecreated = true) }
    }

    /**
     *  If activity resumes and we did not receive a callback from the custom tabs,
     *  then the user hit the back button or closed the custom tabs UI, so return result as
     *  canceled.
     */
    internal fun onResume() {
        if (_state.value.authFlowActive && _state.value.activityRecreated.not()) {
            viewModelScope.launch {
                _viewEffect.emit(FinishWithResult(Result.Canceled))
            }
        }
    }

    /**
     * If activity receives result and we did not receive a callback from the custom tabs,
     * if activity got recreated and the auth flow is still active then the user hit
     * the back button or closed the custom tabs UI, so return result as canceled.
     */
    internal fun onActivityResult() {
        if (_state.value.authFlowActive && _state.value.activityRecreated) {
            viewModelScope.launch {
                _viewEffect.emit(FinishWithResult(Result.Canceled))
            }
        }
    }

    /**
     * For regular connections flows requesting a link account session:
     *
     * On successfully completing the hosted auth flow and receiving the success callback intent,
     * fetch the updated [LinkAccountSession] from the Stripe API,
     * and return that via the [Result] to [com.stripe.android.connections.ConnectionsSheet]
     */
    private fun fetchLinkAccountSession() {
        viewModelScope.launch {
            kotlin.runCatching {
                fetchLinkAccountSession(starterArgs.configuration.linkAccountSessionClientSecret)
            }.onSuccess {
                val result = Result.Completed(it)
                eventReporter.onResult(starterArgs.configuration, result)
                _viewEffect.emit(FinishWithResult(result))
            }.onFailure {
                onFatal(it)
            }
        }
    }

    /**
     * For connections flows requesting an account [com.stripe.android.model.Token]:
     *
     * On successfully completing the hosted auth flow and receiving the success callback intent,
     * fetch the updated [LinkAccountSession] and the generated [com.stripe.android.model.Token]
     * and return that via the [Result] to [com.stripe.android.connections.ConnectionsSheet]
     *
     */
    private fun fetchLinkAccountSessionForToken() {
        viewModelScope.launch {
            kotlin.runCatching {
                fetchLinkAccountSessionForToken(starterArgs.configuration.linkAccountSessionClientSecret)
            }.onSuccess { (las, token) ->
                val result = Result.Completed(las, token)
                eventReporter.onResult(starterArgs.configuration, result)
                _viewEffect.emit(FinishWithResult(result))
            }.onFailure {
                onFatal(it)
            }
        }
    }

    /**
     * If an error occurs during the connections sheet auth flow,
     * returns that error as a [Result.Failed].
     *
     * @param throwable the error encountered during the connections sheet auth flow
     */
    private suspend fun onFatal(throwable: Throwable) {
        val result = Result.Failed(throwable)
        eventReporter.onResult(starterArgs.configuration, result)
        _viewEffect.emit(FinishWithResult(result))
    }

    /**
     * If a user cancels the hosted auth flow either by closing the custom tab with the back button
     * or clicking a cancel link within the hosted auth flow and the activity received the canceled
     * URL callback, finish with [Result.Canceled]
     */
    private suspend fun onUserCancel() {
        val result = Result.Canceled
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
        _state.updateAndPersist { it.copy(authFlowActive = false) }
        viewModelScope.launch {
            val manifest = _state.value.manifest
            when (intent?.data.toString()) {
                manifest?.successUrl -> when (starterArgs) {
                    is Args.Default -> fetchLinkAccountSession()
                    is Args.ForToken -> fetchLinkAccountSessionForToken()
                }
                manifest?.cancelUrl -> onUserCancel()
                else -> onFatal(Exception("Error processing ConnectionsSheet intent"))
            }
        }
    }

    /**
     * Updates state AND saves persistable fields into [SavedStateHandle]
     */
    private inline fun MutableStateFlow<ConnectionsSheetState>.updateAndPersist(
        function: (ConnectionsSheetState) -> ConnectionsSheetState
    ) {
        val previousValue = value
        update(function)
        value.to(savedStateHandle, previousValue)
    }

    class Factory(
        private val applicationSupplier: () -> Application,
        private val starterArgsSupplier: () -> Args,
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle? = null
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            savedStateHandle: SavedStateHandle
        ): T {
            return DaggerConnectionsSheetComponent
                .builder()
                .application(applicationSupplier())
                .savedStateHandle(savedStateHandle)
                .configuration(starterArgsSupplier())
                .build().viewModel as T
        }
    }

    internal companion object {
        internal const val MAX_ACCOUNTS = 100
    }
}
