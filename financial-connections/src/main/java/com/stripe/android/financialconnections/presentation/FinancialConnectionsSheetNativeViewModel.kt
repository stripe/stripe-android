package com.stripe.android.financialconnections.presentation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.PersistState
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ClickNavBarBack
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ClickNavBarClose
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Complete
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.di.DaggerFinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.CompleteFinancialConnectionsSession
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Terminate
import com.stripe.android.financialconnections.exception.CustomManualEntryRequiredError
import com.stripe.android.financialconnections.exception.WebAuthFlowCancelledException
import com.stripe.android.financialconnections.exception.WebAuthFlowFailedException
import com.stripe.android.financialconnections.features.consent.ConsentTextBuilder
import com.stripe.android.financialconnections.features.manualentry.isCustomManualEntryError
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Canceled
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Completed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Failed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.NETWORKING_LINK_SIGNUP_PANE
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeState.CloseDialog
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.Finish
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.OpenUrl
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.utils.UriUtils
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@Suppress("LongParameterList", "TooManyFunctions")
internal class FinancialConnectionsSheetNativeViewModel @Inject constructor(
    /**
     * Exposes parent dagger component (activity viewModel scoped so that it survives config changes)
     * No other dependencies should be exposed from the viewModel
     */
    val activityRetainedComponent: FinancialConnectionsSheetNativeComponent,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val getManifest: GetManifest,
    private val uriUtils: UriUtils,
    private val completeFinancialConnectionsSession: CompleteFinancialConnectionsSession,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val logger: Logger,
    @Named(APPLICATION_ID) private val applicationId: String,
    initialState: FinancialConnectionsSheetNativeState
) : MavericksViewModel<FinancialConnectionsSheetNativeState>(initialState) {

    init {
        setState { copy(firstInit = false) }
        viewModelScope.launch {
            nativeAuthFlowCoordinator().collect { message ->
                when (message) {
                    is Message.Finish -> {
                        setState { copy(viewEffect = Finish(message.result)) }
                    }

                    Message.ClearPartnerWebAuth -> {
                        setState { copy(webAuthFlow = Uninitialized) }
                    }

                    is Terminate -> closeAuthFlow(
                        earlyTerminationCause = message.cause
                    )
                }
            }
        }
    }

    /**
     * When authorization flow finishes, it will redirect to a URL scheme stripe-auth://link-accounts
     * captured by [com.stripe.android.financialconnections.FinancialConnectionsSheetRedirectActivity]
     * that will launch this activity in `singleTask` mode.
     *
     * @param intent the new intent with the redirect URL in the intent data
     */
    fun handleOnNewIntent(intent: Intent?) {
        viewModelScope.launch {
            val receivedUrl: String = intent?.data?.toString() ?: ""
            when {
                receivedUrl.contains("authentication_return", true) -> {
                    setState {
                        copy(webAuthFlow = Success(receivedUrl))
                    }
                }

                uriUtils.compareSchemeAuthorityAndPath(
                    receivedUrl,
                    baseUrl(applicationId)
                ) -> when (uriUtils.getQueryParameter(receivedUrl, PARAM_STATUS)) {
                    STATUS_SUCCESS -> setState {
                        copy(webAuthFlow = Success(receivedUrl))
                    }

                    STATUS_CANCEL -> setState {
                        copy(webAuthFlow = Fail(WebAuthFlowCancelledException()))
                    }

                    STATUS_FAILURE -> setState {
                        copy(
                            webAuthFlow = Fail(
                                WebAuthFlowFailedException(
                                    message = "Received return_url with failed status: $receivedUrl",
                                    reason = uriUtils.getQueryParameter(
                                        receivedUrl,
                                        PARAM_ERROR_REASON
                                    )
                                )
                            )
                        )
                    }

                    // received unknown / non-handleable [PARAM_STATUS]
                    else -> setState {
                        copy(
                            webAuthFlow = Fail(
                                WebAuthFlowFailedException(
                                    message = "Received return_url with unknown status: $receivedUrl",
                                    reason = null
                                )
                            )
                        )
                    }
                }
                // received unknown / non-handleable return url.
                else -> setState {
                    copy(
                        webAuthFlow = Fail(
                            WebAuthFlowFailedException(
                                message = "Received unknown return_url: $receivedUrl",
                                reason = null
                            )
                        )
                    )
                }
            }
        }
    }

    /**
     *  If activity resumes and we did not receive a callback from the AuthFlow,
     *  then the user hit the back button or closed the custom tabs UI, so return result as
     *  canceled.
     */
    fun onResume() {
        setState {
            if (webAuthFlow is Loading) {
                copy(webAuthFlow = Fail(WebAuthFlowCancelledException()))
            } else {
                this
            }
        }
    }

    fun openPartnerAuthFlowInBrowser(url: String) {
        setState {
            copy(
                webAuthFlow = Loading(),
                viewEffect = OpenUrl(url)
            )
        }
    }

    fun onViewEffectLaunched() {
        setState { copy(viewEffect = null) }
    }

    fun onCloseWithConfirmationClick(pane: Pane) = viewModelScope.launch {
        val manifest = kotlin.runCatching { getManifest() }.getOrNull()
        val businessName = manifest?.let(ConsentTextBuilder::getBusinessName)
        val isNetworkingSignupPane =
            manifest?.isNetworkingUserFlow == true && pane == NETWORKING_LINK_SIGNUP_PANE
        val description = when {
            isNetworkingSignupPane && businessName != null -> TextResource.StringId(
                value = R.string.stripe_close_dialog_networking_desc,
                args = listOf(businessName)
            )
            else -> TextResource.StringId(
                value = R.string.stripe_close_dialog_desc
            )
        }
        eventTracker.track(ClickNavBarClose(pane))
        setState { copy(closeDialog = CloseDialog(description = description)) }
    }

    fun onBackClick(pane: Pane) {
        viewModelScope.launch {
            eventTracker.track(ClickNavBarBack(pane))
        }
    }

    fun onCloseNoConfirmationClick(pane: Pane) {
        viewModelScope.launch {
            eventTracker.track(ClickNavBarClose(pane))
        }
        closeAuthFlow(closeAuthFlowError = null)
    }

    fun onCloseFromErrorClick(error: Throwable) = closeAuthFlow(
        closeAuthFlowError = error
    )

    fun onCloseConfirm() = closeAuthFlow(
        closeAuthFlowError = null
    )

    fun onCloseDismiss() = setState { copy(closeDialog = null) }

    /**
     * [NavHost] handles back presses except for when backstack is empty, where it delegates
     * to the container activity. [onBackPressed] will be triggered on these empty backstack cases.
     */
    fun onBackPressed() {
        closeAuthFlow(closeAuthFlowError = null)
    }

    /**
     * There's at least three types of close cases:
     * 1. User closes (with or without an error),
     *    and fetching accounts returns accounts (or `paymentAccount`). That's a success.
     * 2. User closes with an error, and fetching accounts returns NO accounts. That's an error.
     * 3. User closes without an error, and fetching accounts returns NO accounts. That's a cancel.
     */
    private fun closeAuthFlow(
        earlyTerminationCause: Terminate.EarlyTerminationCause? = null,
        closeAuthFlowError: Throwable? = null
    ) {
        viewModelScope.launch {
            kotlin
                .runCatching {
                    completeFinancialConnectionsSession(
                        terminalError = earlyTerminationCause?.value
                    )
                }
                .onSuccess { session ->
                    eventTracker.track(
                        Complete(
                            exception = null,
                            connectedAccounts = session.accounts.data.count()
                        )
                    )
                    when {
                        session.isCustomManualEntryError() -> {
                            val result = Failed(CustomManualEntryRequiredError())
                            setState { copy(viewEffect = Finish(result)) }
                        }

                        session.accounts.data.isNotEmpty() ||
                            session.paymentAccount != null ||
                            session.bankAccountToken != null -> {
                            val result = Completed(
                                financialConnectionsSession = session,
                                token = session.parsedToken
                            )
                            setState { copy(viewEffect = Finish(result)) }
                        }

                        closeAuthFlowError != null -> setState {
                            copy(viewEffect = Finish(Failed(closeAuthFlowError)))
                        }

                        else -> setState {
                            copy(viewEffect = Finish(Canceled))
                        }
                    }
                }
                .onFailure { completeSessionError ->
                    logger.error("Error completing session before closing", completeSessionError)
                    eventTracker.track(
                        Complete(
                            exception = completeSessionError,
                            connectedAccounts = null
                        )
                    )
                    setState {
                        copy(
                            viewEffect = Finish(
                                Failed(closeAuthFlowError ?: completeSessionError)
                            )
                        )
                    }
                }
        }
    }

    fun onPaneLaunched(pane: Pane) {
        viewModelScope.launch {
            eventTracker.track(
                FinancialConnectionsEvent.PaneLaunched(pane)
            )
        }
    }

    companion object :
        MavericksViewModelFactory<FinancialConnectionsSheetNativeViewModel, FinancialConnectionsSheetNativeState> {

        private fun baseUrl(applicationId: String) =
            "stripe://auth-redirect/$applicationId"

        private const val PARAM_STATUS = "status"
        private const val PARAM_ERROR_REASON = "error_reason"
        private const val STATUS_SUCCESS = "success"
        private const val STATUS_CANCEL = "cancel"
        private const val STATUS_FAILURE = "failure"

        override fun create(
            viewModelContext: ViewModelContext,
            state: FinancialConnectionsSheetNativeState
        ): FinancialConnectionsSheetNativeViewModel {
            val args = viewModelContext.args<FinancialConnectionsSheetNativeActivityArgs>()
            return DaggerFinancialConnectionsSheetNativeComponent
                .builder()
                .initialSyncResponse(args.initialSyncResponse.takeIf { state.firstInit })
                .application(viewModelContext.app())
                .configuration(state.configuration)
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class FinancialConnectionsSheetNativeState(
    val webAuthFlow: Async<String>,
    /**
     * Tracks whether this state was recreated from a process kill.
     */
    @PersistState
    val firstInit: Boolean,
    val configuration: FinancialConnectionsSheet.Configuration,
    val closeDialog: CloseDialog?,
    val reducedBranding: Boolean,
    val viewEffect: FinancialConnectionsSheetNativeViewEffect?,
    val initialPane: Pane
) : MavericksState {

    /**
     * Payload for the close confirmation dialog,
     * which is shown when the user clicks the close button.
     */
    data class CloseDialog(
        val description: TextResource,
    )

    /**
     * Used by Mavericks to build initial state based on args.
     */
    @Suppress("Unused")
    constructor(args: FinancialConnectionsSheetNativeActivityArgs) : this(
        webAuthFlow = Uninitialized,
        reducedBranding = args.initialSyncResponse.visual.reducedBranding,
        firstInit = true,
        initialPane = args.initialSyncResponse.manifest.nextPane,
        configuration = args.configuration,
        closeDialog = null,
        viewEffect = null
    )
}

@Composable
internal fun parentViewModel(): FinancialConnectionsSheetNativeViewModel =
    mavericksActivityViewModel()

internal sealed interface FinancialConnectionsSheetNativeViewEffect {
    /**
     * Open the Web AuthFlow.
     */
    data class OpenUrl(
        val url: String
    ) : FinancialConnectionsSheetNativeViewEffect

    /**
     * Finish the container activity.
     */
    data class Finish(
        val result: FinancialConnectionsSheetActivityResult
    ) : FinancialConnectionsSheetNativeViewEffect
}
