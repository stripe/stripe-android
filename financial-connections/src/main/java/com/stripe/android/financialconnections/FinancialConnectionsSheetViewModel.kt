package com.stripe.android.financialconnections

import android.content.Intent
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.FinishWithResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenNativeAuthFlow
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEventReporter
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.di.DaggerFinancialConnectionsSheetComponent
import com.stripe.android.financialconnections.domain.FetchFinancialConnectionsSession
import com.stripe.android.financialconnections.domain.FetchFinancialConnectionsSessionForToken
import com.stripe.android.financialconnections.domain.GenerateFinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Canceled
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@Suppress("LongParameterList", "TooManyFunctions")
internal class FinancialConnectionsSheetViewModel @Inject constructor(
    @Named(APPLICATION_ID) private val applicationId: String,
    private val generateFinancialConnectionsSessionManifest: GenerateFinancialConnectionsSessionManifest,
    private val fetchFinancialConnectionsSession: FetchFinancialConnectionsSession,
    private val fetchFinancialConnectionsSessionForToken: FetchFinancialConnectionsSessionForToken,
    private val eventReporter: FinancialConnectionsEventReporter,
    initialState: FinancialConnectionsSheetState
) : MavericksViewModel<FinancialConnectionsSheetState>(initialState) {

    init {
        eventReporter.onPresented(initialState.initialArgs.configuration)
        // avoid re-fetching manifest if already exists (this will happen on process recreations)
        if (initialState.manifest == null) {
            fetchManifest()
        }
    }

    /**
     * Fetches the [FinancialConnectionsSessionManifest] from the Stripe API to get the hosted auth flow URL
     * as well as the success and cancel callback URLs to verify.
     */
    private fun fetchManifest() {
        withState { state ->
            viewModelScope.launch {
                kotlin.runCatching {
                    state.initialArgs.validate()
                    generateFinancialConnectionsSessionManifest(
                        clientSecret = state.sessionSecret,
                        applicationId = applicationId
                    )
                }.onFailure {
                    onFatal(state, it)
                }.onSuccess {
                    openAuthFlow(it)
                }
            }
        }
    }

    /**
     * Builds the ChromeCustomTab intent to launch the hosted auth flow and launches it.
     *
     * @param manifest the manifest containing the hosted auth flow URL to launch
     *
     */
    private fun openAuthFlow(manifest: FinancialConnectionsSessionManifest) {
        // stores manifest in state for future references.
        setState {
            // TODO@carlosmuvi implement manifest-based logic to open the corresponding flow.
            val nativeAuthFlow = true
            copy(
                manifest = manifest,
                authFlowActive = true,
                viewEffect = if (nativeAuthFlow) {
                    OpenNativeAuthFlow(initialArgs.configuration, manifest)
                } else {
                    OpenAuthFlowWithUrl(manifest.hostedAuthUrl)
                }
            )
        }
    }

    /**
     * Activity recreation changes the lifecycle order:
     *
     * If config change happens while in web flow:
     * - onResume -> onNewIntent -> activityResult -> onResume(again)
     * If no config change happens:
     * - onActivityResult -> onNewIntent -> onResume
     *
     * (note [handleOnNewIntent] will just get called if user completed the web flow and clicked
     * the deeplink that redirects back to the app)
     *
     * We need to rely on a post-onNewIntent lifecycle callback to figure if the user completed
     * or cancelled the web flow. [FinancialConnectionsSheetState.activityRecreated] will be used to
     * figure which lifecycle callback happens after onNewIntent.
     *
     * @see onResume (we rely on this on regular flows)
     * @see onBrowserActivityResult (we rely on this on config changes)
     */
    fun onActivityRecreated() {
        setState {
            copy(
                activityRecreated = true
            )
        }
    }

    /**
     *  If activity resumes and we did not receive a callback from the custom tabs,
     *  then the user hit the back button or closed the custom tabs UI, so return result as
     *  canceled.
     */
    internal fun onResume() {
        setState {
            if (authFlowActive && activityRecreated.not()) {
                copy(viewEffect = FinishWithResult(Canceled))
            } else this
        }
    }

    /**
     * If activity receives result and we did not receive a callback from the custom tabs,
     * if activity got recreated and the auth flow is still active then the user hit
     * the back button or closed the custom tabs UI, so return result as canceled.
     */
    internal fun onBrowserActivityResult() {
        setState {
            if (authFlowActive && activityRecreated) {
                copy(viewEffect = FinishWithResult(Canceled))
            } else this
        }
    }

    /**
     * For regular connections flows requesting a session:
     *
     * On successfully completing the hosted auth flow and receiving the success callback intent,
     * fetch the updated [FinancialConnectionsSession] model from the API
     * and return it back as a [Completed] result.
     */
    private fun fetchFinancialConnectionsSession(state: FinancialConnectionsSheetState) {
        viewModelScope.launch {
            kotlin.runCatching {
                fetchFinancialConnectionsSession(state.sessionSecret)
            }.onSuccess {
                val result = FinancialConnectionsSheetActivityResult.Completed(it)
                eventReporter.onResult(state.initialArgs.configuration, result)
                setState { copy(viewEffect = FinishWithResult(result)) }
            }.onFailure {
                onFatal(state, it)
            }
        }
    }

    /**
     * For connections flows requesting an account [com.stripe.android.model.Token]:
     *
     * On successfully completing the hosted auth flow and receiving the success callback intent,
     * fetch the updated [FinancialConnectionsSession] and the generated [com.stripe.android.model.Token]
     * and return it back as a [Completed] result.
     */
    private fun fetchFinancialConnectionsSessionForToken(state: FinancialConnectionsSheetState) {
        viewModelScope.launch {
            kotlin.runCatching {
                fetchFinancialConnectionsSessionForToken(clientSecret = state.sessionSecret)
            }.onSuccess { (las, token) ->
                val result = FinancialConnectionsSheetActivityResult.Completed(las, token)
                eventReporter.onResult(state.initialArgs.configuration, result)
                setState { copy(viewEffect = FinishWithResult(result)) }
            }.onFailure {
                onFatal(state, it)
            }
        }
    }

    /**
     * If an error occurs during the auth flow, return that error via the
     * [FinancialConnectionsSheetResultCallback] and [FinancialConnectionsSheetResult.Failed].
     *
     * @param throwable the error encountered during the [FinancialConnectionsSheet] auth flow
     */
    private fun onFatal(state: FinancialConnectionsSheetState, throwable: Throwable) {
        val result = FinancialConnectionsSheetActivityResult.Failed(throwable)
        eventReporter.onResult(state.initialArgs.configuration, result)
        setState { copy(viewEffect = FinishWithResult(result)) }
    }

    /**
     * If a user cancels the hosted auth flow either by closing the custom tab with the back button
     * or clicking a cancel link within the hosted auth flow and the activity received the canceled
     * URL callback, notify the [FinancialConnectionsSheetResultCallback] with [Canceled]
     */
    private fun onUserCancel(state: FinancialConnectionsSheetState) {
        val result = Canceled
        eventReporter.onResult(state.initialArgs.configuration, result)
        setState { copy(viewEffect = FinishWithResult(result)) }
    }

    /**
     * The hosted auth flow will redirect to a URL scheme stripe-auth://link-accounts which will be
     * handled by the [FinancialConnectionsSheetActivity] per the intent filter in the Android manifest and
     * with the launch mode for the activity being `singleTask` it will trigger a new intent for the
     * activity which this method will receive
     *
     * @param intent the new intent with the redirect URL in the intent data
     */
    internal fun handleOnNewIntent(intent: Intent?) {
        setState { copy(authFlowActive = false) }
        withState { state ->
            when (intent?.data.toString()) {
                state.manifest?.successUrl -> when (state.initialArgs) {
                    is FinancialConnectionsSheetActivityArgs.ForData ->
                        fetchFinancialConnectionsSession(state)
                    is FinancialConnectionsSheetActivityArgs.ForToken ->
                        fetchFinancialConnectionsSessionForToken(state)
                }
                state.manifest?.cancelUrl -> onUserCancel(state)
                else ->
                    onFatal(state, Exception("Error processing FinancialConnectionsSheet intent"))
            }
        }
    }

    fun onViewEffectLaunched() {
        setState { copy(viewEffect = null) }
    }

    fun onNativeAuthFlowResult() {
        setState {
            if (authFlowActive && activityRecreated) {
                copy(viewEffect = FinishWithResult(Canceled))
            } else this
        }
    }

    companion object :
        MavericksViewModelFactory<FinancialConnectionsSheetViewModel, FinancialConnectionsSheetState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: FinancialConnectionsSheetState
        ): FinancialConnectionsSheetViewModel {
            return DaggerFinancialConnectionsSheetComponent
                .builder()
                .application(viewModelContext.app())
                .initialState(state)
                .configuration(state.initialArgs.configuration)
                .build().viewModel
        }

        internal const val MAX_ACCOUNTS = 100
    }
}
