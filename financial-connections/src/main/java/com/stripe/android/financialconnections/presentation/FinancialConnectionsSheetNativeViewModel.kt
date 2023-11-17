package com.stripe.android.financialconnections.presentation

import android.content.Intent
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination
import androidx.navigation.compose.NavHost
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.PersistState
import com.airbnb.mvrx.ViewModelContext
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.AppBackgrounded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ClickNavBarBack
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ClickNavBarClose
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Complete
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLaunched
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Metadata
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.di.DaggerFinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.CompleteFinancialConnectionsSession
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Complete.EarlyTerminationCause
import com.stripe.android.financialconnections.exception.CustomManualEntryRequiredError
import com.stripe.android.financialconnections.features.common.getBusinessName
import com.stripe.android.financialconnections.features.manualentry.isCustomManualEntryError
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Canceled
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Completed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Failed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.NETWORKING_LINK_SIGNUP_PANE
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.UNEXPECTED_ERROR
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.pane
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeState.CloseDialog
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.Finish
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.OpenUrl
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.utils.UriUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.parcelize.Parcelize
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
    navigationManager: NavigationManager,
    initialState: FinancialConnectionsSheetNativeState
) : MavericksViewModel<FinancialConnectionsSheetNativeState>(initialState) {

    private val mutex = Mutex()
    val navigationFlow = navigationManager.navigationFlow

    init {
        setState { copy(firstInit = false) }
        viewModelScope.launch {
            nativeAuthFlowCoordinator().collect { message ->
                when (message) {
                    Message.ClearPartnerWebAuth -> {
                        setState { copy(webAuthFlow = WebAuthFlowState.Uninitialized) }
                    }

                    is Message.Complete -> closeAuthFlow(
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
    fun handleOnNewIntent(intent: Intent?) = viewModelScope.launch {
        mutex.withLock {
            val receivedUrl: String = intent?.data?.toString() ?: ""
            when {
                // App2App: status comes as a query parameter in the fragment section of the url.
                receivedUrl.contains("authentication_return", true) -> onUrlReceived(
                    receivedUrl = receivedUrl,
                    status = uriUtils.getQueryParameterFromFragment(receivedUrl, PARAM_CODE)
                )

                // Regular return url: status comes as a query parameter.
                uriUtils.compareSchemeAuthorityAndPath(
                    receivedUrl,
                    baseUrl(applicationId)
                ) -> onUrlReceived(
                    receivedUrl = receivedUrl,
                    status = uriUtils.getQueryParameter(receivedUrl, PARAM_STATUS)
                )
                // received unknown / non-handleable return url.
                else -> setState {
                    copy(webAuthFlow = WebAuthFlowState.Canceled(receivedUrl))
                }
            }
        }
    }

    private suspend fun onUrlReceived(receivedUrl: String, status: String?) {
        when (status) {
            STATUS_SUCCESS -> setState {
                copy(webAuthFlow = WebAuthFlowState.Success(receivedUrl))
            }

            STATUS_FAILURE -> {
                val reason = uriUtils.getQueryParameter(receivedUrl, PARAM_ERROR_REASON)
                setState {
                    copy(
                        webAuthFlow = WebAuthFlowState.Failed(
                            url = receivedUrl,
                            message = "Received return_url with failed status: $receivedUrl",
                            reason = reason
                        )
                    )
                }
            }

            // received cancel / unknown / non-handleable [PARAM_STATUS]
            else -> setState {
                copy(webAuthFlow = WebAuthFlowState.Canceled(receivedUrl))
            }
        }
    }

    /**
     *  If activity resumes and we did not receive a callback from the AuthFlow,
     *  then the user hit the back button or closed the custom tabs UI, so return result as
     *  canceled.
     */
    fun onResume() = viewModelScope.launch {
        mutex.withLock {
            val state = awaitState()
            if (state.webAuthFlow is WebAuthFlowState.InProgress) {
                setState { copy(webAuthFlow = WebAuthFlowState.Canceled(url = null)) }
            }
        }
    }

    fun openPartnerAuthFlowInBrowser(url: String) {
        setState {
            copy(
                webAuthFlow = WebAuthFlowState.InProgress,
                viewEffect = OpenUrl(url)
            )
        }
    }

    fun onViewEffectLaunched() {
        setState { copy(viewEffect = null) }
    }

    fun onCloseWithConfirmationClick(pane: Pane) = viewModelScope.launch {
        val manifest = kotlin.runCatching { getManifest() }.getOrNull()
        val businessName = manifest?.getBusinessName()
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

    fun onBackClick(pane: Pane?) {
        viewModelScope.launch {
            pane?.let { eventTracker.track(ClickNavBarBack(pane)) }
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
        earlyTerminationCause: EarlyTerminationCause? = null,
        closeAuthFlowError: Throwable? = null
    ) = viewModelScope.launch {
        mutex.withLock {
            // prevents multiple complete triggers.
            if (awaitState().completed) return@launch
            setState { copy(completed = true) }
            runCatching {
                val session = completeFinancialConnectionsSession(earlyTerminationCause?.value)
                eventTracker.track(
                    Complete(
                        exception = null,
                        exceptionExtraMessage = null,
                        connectedAccounts = session.accounts.data.count()
                    )
                )
                when {
                    session.isCustomManualEntryError() -> {
                        FinancialConnections.emitEvent(Name.MANUAL_ENTRY_INITIATED)
                        finishWithResult(
                            Failed(error = CustomManualEntryRequiredError())
                        )
                    }

                    session.hasAValidAccount() -> {
                        FinancialConnections.emitEvent(
                            name = Name.SUCCESS,
                            metadata = Metadata(
                                manualEntry = session.paymentAccount is BankAccount,
                            )
                        )
                        finishWithResult(
                            Completed(
                                financialConnectionsSession = session,
                                token = session.parsedToken
                            )
                        )
                    }

                    closeAuthFlowError != null -> finishWithResult(
                        Failed(error = closeAuthFlowError)
                    )

                    else -> {
                        FinancialConnections.emitEvent(Name.CANCEL)
                        finishWithResult(Canceled)
                    }
                }
            }.onFailure { completeSessionError ->
                val errorMessage = "Error completing session before closing"
                logger.error(errorMessage, completeSessionError)
                eventTracker.track(
                    Complete(
                        exception = completeSessionError,
                        exceptionExtraMessage = errorMessage,
                        connectedAccounts = null
                    )
                )
                finishWithResult(Failed(closeAuthFlowError ?: completeSessionError))
            }
        }
    }

    private fun finishWithResult(
        result: FinancialConnectionsSheetActivityResult
    ) {
        setState { copy(viewEffect = Finish(result)) }
    }

    private fun FinancialConnectionsSession.hasAValidAccount() =
        accounts.data.isNotEmpty() ||
            paymentAccount != null ||
            bankAccountToken != null

    fun onPaneLaunched(pane: Pane, referrer: Pane?) {
        viewModelScope.launch {
            eventTracker.track(
                PaneLaunched(
                    referrer = referrer,
                    pane = pane
                )
            )
        }
    }

    fun onBackgrounded(currentDestination: NavDestination?, backgrounded: Boolean) {
        viewModelScope.launch {
            eventTracker.track(
                AppBackgrounded(
                    pane = currentDestination?.pane ?: UNEXPECTED_ERROR,
                    backgrounded = backgrounded
                )
            )
        }
    }

    companion object :
        MavericksViewModelFactory<FinancialConnectionsSheetNativeViewModel, FinancialConnectionsSheetNativeState> {

        private fun baseUrl(applicationId: String) =
            "stripe://auth-redirect/$applicationId"

        private const val PARAM_STATUS = "status"
        private const val PARAM_CODE = "code"
        private const val PARAM_ERROR_REASON = "error_reason"
        private const val STATUS_SUCCESS = "success"
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
    @PersistState
    val webAuthFlow: WebAuthFlowState,
    /**
     * Tracks whether this state was recreated from a process kill.
     */
    @PersistState
    val firstInit: Boolean,
    val configuration: FinancialConnectionsSheet.Configuration,
    val closeDialog: CloseDialog?,
    val reducedBranding: Boolean,
    val viewEffect: FinancialConnectionsSheetNativeViewEffect?,
    val completed: Boolean,
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
        webAuthFlow = WebAuthFlowState.Uninitialized,
        reducedBranding = args.initialSyncResponse.visual.reducedBranding,
        firstInit = true,
        completed = false,
        initialPane = args.initialSyncResponse.manifest.nextPane,
        configuration = args.configuration,
        closeDialog = null,
        viewEffect = null
    )
}

/**
 * Authentication with an institution opens on an external browser.
 *
 * This state tracks the status of the authentication flow in the browser.
 */
internal sealed class WebAuthFlowState : Parcelable {
    /**
     * The web browser has not been opened yet.
     */
    @Parcelize
    object Uninitialized : WebAuthFlowState()

    /**
     * The web browser has been opened and the authentication flow is in progress.
     */
    @Parcelize
    object InProgress : WebAuthFlowState()

    /**
     * The web browser has been closed and triggered a deeplink with a success result.
     */
    @Parcelize
    data class Success(
        val url: String
    ) : WebAuthFlowState()

    /**
     * The web browser has been closed with no deeplink,
     * and the authentication flow is considered as canceled.
     */
    @Parcelize
    data class Canceled(
        val url: String?
    ) : WebAuthFlowState()

    /**
     * The web browser has been closed and triggered a deeplink with a failure result,
     * or something else went wrong (unreadable / unknown structure of the received deeplink)
     */
    @Parcelize
    data class Failed(
        val url: String,
        val message: String,
        val reason: String?
    ) : WebAuthFlowState()
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
