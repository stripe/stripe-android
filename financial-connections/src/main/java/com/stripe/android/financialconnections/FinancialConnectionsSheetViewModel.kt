package com.stripe.android.financialconnections

import android.content.Intent
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.FinishWithResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenAuthFlowWithUrl
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
    private val starterArgs: FinancialConnectionsSheetActivityArgs,
    private val generateFinancialConnectionsSessionManifest: GenerateFinancialConnectionsSessionManifest,
    private val fetchFinancialConnectionsSession: FetchFinancialConnectionsSession,
    private val fetchFinancialConnectionsSessionForToken: FetchFinancialConnectionsSessionForToken,
    private val eventReporter: FinancialConnectionsEventReporter,
    private val initialState: FinancialConnectionsSheetState
) : MavericksViewModel<FinancialConnectionsSheetState>(initialState) {

    init {
        eventReporter.onPresented(starterArgs.configuration)
        withState {
            // avoid re-fetching manifest if already exists (this will happen on process recreations)
            if (it.manifest == null) {
                fetchManifest()
            }
        }
    }

    /**
     * Fetches the [FinancialConnectionsSessionManifest] from the Stripe API to get the hosted auth flow URL
     * as well as the success and cancel callback URLs to verify.
     */
    private fun fetchManifest() {
        viewModelScope.launch {
            kotlin.runCatching {
                generateFinancialConnectionsSessionManifest(
                    clientSecret = starterArgs.configuration.financialConnectionsSessionClientSecret,
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
    private suspend fun openAuthFlow(manifest: FinancialConnectionsSessionManifest) {
        // stores manifest in state for future references.
        setState {
            copy(
                manifest = manifest,
                authFlowActive = true,
                sideEffect = Success(OpenAuthFlowWithUrl(manifest.hostedAuthUrl))
            )
        }
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
     * or cancelled the web flow. [FinancialConnectionsSheetState.activityRecreated] will be used to
     * figure which lifecycle callback happens after onNewIntent.
     *
     * @see onResume (we rely on this on regular flows)
     * @see onActivityResult (we rely on this on config changes)
     */
    internal fun onActivityRecreated() {
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
                copy(
                    sideEffect = Success(FinishWithResult(Canceled))
                )
            } else this
        }
    }

    /**
     * If activity receives result and we did not receive a callback from the custom tabs,
     * if activity got recreated and the auth flow is still active then the user hit
     * the back button or closed the custom tabs UI, so return result as canceled.
     */
    internal fun onActivityResult() {
        setState {
            if (authFlowActive && activityRecreated) {
                copy(
                    sideEffect = Success(FinishWithResult(Canceled))
                )
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
    private fun fetchFinancialConnectionsSession() {
        viewModelScope.launch {
            kotlin.runCatching {
                fetchFinancialConnectionsSession(starterArgs.configuration.financialConnectionsSessionClientSecret)
            }.onSuccess {
                val result = FinancialConnectionsSheetActivityResult.Completed(it)
                eventReporter.onResult(starterArgs.configuration, result)
                setState { copy(sideEffect = Success(FinishWithResult(result))) }
            }.onFailure {
                onFatal(it)
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
    private fun fetchFinancialConnectionsSessionForToken() {
        viewModelScope.launch {
            kotlin.runCatching {
                fetchFinancialConnectionsSessionForToken(
                    clientSecret = starterArgs.configuration.financialConnectionsSessionClientSecret
                )
            }.onSuccess { (las, token) ->
                val result = FinancialConnectionsSheetActivityResult.Completed(las, token)
                eventReporter.onResult(starterArgs.configuration, result)
                setState { copy(sideEffect = Success(FinishWithResult(result))) }
            }.onFailure {
                onFatal(it)
            }
        }
    }

    /**
     * If an error occurs during the auth flow, return that error via the
     * [FinancialConnectionsSheetResultCallback] and [FinancialConnectionsSheetResult.Failed].
     *
     * @param throwable the error encountered during the [FinancialConnectionsSheet] auth flow
     */
    private suspend fun onFatal(throwable: Throwable) {
        val result = FinancialConnectionsSheetActivityResult.Failed(throwable)
        eventReporter.onResult(starterArgs.configuration, result)
        setState { copy(sideEffect = Success(FinishWithResult(result))) }
    }

    /**
     * If a user cancels the hosted auth flow either by closing the custom tab with the back button
     * or clicking a cancel link within the hosted auth flow and the activity received the canceled
     * URL callback, notify the [FinancialConnectionsSheetResultCallback] with [Canceled]
     */
    private suspend fun onUserCancel() {
        val result = Canceled
        eventReporter.onResult(starterArgs.configuration, result)
        setState { copy(sideEffect = Success(FinishWithResult(result))) }
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
        withState {
            when (intent?.data.toString()) {
                it.manifest?.successUrl -> when (starterArgs) {
                    is FinancialConnectionsSheetActivityArgs.ForData -> fetchFinancialConnectionsSession()
                    is FinancialConnectionsSheetActivityArgs.ForToken -> fetchFinancialConnectionsSessionForToken()
                }
                it.manifest?.cancelUrl -> viewModelScope.launch { onUserCancel() }
                else -> viewModelScope.launch { onFatal(Exception("Error processing FinancialConnectionsSheet intent"))  }
            }
        }
    }

    companion object : MavericksViewModelFactory<FinancialConnectionsSheetViewModel, FinancialConnectionsSheetState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: FinancialConnectionsSheetState
        ): FinancialConnectionsSheetViewModel {
            return DaggerFinancialConnectionsSheetComponent
                .builder()
                .application(viewModelContext.app())
                .initialState(state)
                .internalArgs(state.initialArgs)
                .build().viewModel
        }

        internal const val MAX_ACCOUNTS = 100
    }
}
