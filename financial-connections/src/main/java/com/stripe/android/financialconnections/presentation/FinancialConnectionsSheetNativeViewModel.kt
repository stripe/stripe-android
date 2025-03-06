package com.stripe.android.financialconnections.presentation

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.AppBackgrounded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ClickNavBarBack
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ClickNavBarClose
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Complete
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLaunched
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Metadata
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.di.ActivityRetainedScope
import com.stripe.android.financialconnections.di.DaggerFinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.di.FinancialConnectionsSingletonSharedComponentHolder
import com.stripe.android.financialconnections.domain.CompleteFinancialConnectionsSession
import com.stripe.android.financialconnections.domain.CreateInstantDebitsResult
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Complete.EarlyTerminationCause
import com.stripe.android.financialconnections.exception.CustomManualEntryRequiredError
import com.stripe.android.financialconnections.exception.FinancialConnectionsError
import com.stripe.android.financialconnections.exception.UnclassifiedError
import com.stripe.android.financialconnections.features.error.FinancialConnectionsAttestationError
import com.stripe.android.financialconnections.features.manualentry.isCustomManualEntryError
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Canceled
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Completed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Failed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.update
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarHost
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarState
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeState.Companion.KEY_FIRST_INIT
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeState.Companion.KEY_SAVED_STATE
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeState.Companion.KEY_WEB_AUTH_FLOW
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.Finish
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.OpenUrl
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity.Companion.getArgs
import com.stripe.android.financialconnections.ui.theme.Theme
import com.stripe.android.financialconnections.ui.toLocalTheme
import com.stripe.android.financialconnections.utils.UriUtils
import com.stripe.android.financialconnections.utils.get
import com.stripe.android.financialconnections.utils.updateWithNewEntry
import com.stripe.android.uicore.navigation.NavigationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Named

@ActivityRetainedScope
internal class FinancialConnectionsSheetNativeViewModel @Inject constructor(
    /**
     * Exposes parent dagger component (activity viewModel scoped so that it survives config changes)
     * No other dependencies should be exposed from the viewModel
     */
    val activityRetainedComponent: FinancialConnectionsSheetNativeComponent,
    savedStateHandle: SavedStateHandle,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val uriUtils: UriUtils,
    private val completeFinancialConnectionsSession: CompleteFinancialConnectionsSession,
    private val createInstantDebitsResult: CreateInstantDebitsResult,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val logger: Logger,
    private val navigationManager: NavigationManager,
    @Named(APPLICATION_ID) private val applicationId: String,
    initialState: FinancialConnectionsSheetNativeState,
) : FinancialConnectionsViewModel<FinancialConnectionsSheetNativeState>(
    initialState,
    nativeAuthFlowCoordinator
),
    TopAppBarHost {

    private val mutex = Mutex()
    val navigationFlow = navigationManager.navigationFlow

    private val defaultTopAppBarState: TopAppBarState by lazy {
        // The first pane may choose to hide the Stripe logo. Therefore, let's hide it by default
        // on the first pane.
        initialState.toTopAppBarState(forceHideStripeLogo = true)
    }

    private val currentPane = MutableStateFlow(initialState.initialPane)
    private val topAppBarStateUpdatesByPane = MutableStateFlow(
        value = mapOf(initialState.initialPane to defaultTopAppBarState),
    )

    val topAppBarState: StateFlow<TopAppBarState> = topAppBarStateUpdatesByPane.get(
        keyFlow = currentPane,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = defaultTopAppBarState,
    )

    init {
        savedStateHandle.registerSavedStateProvider()
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

                    is Message.CloseWithError -> closeAuthFlow(
                        closeAuthFlowError = message.cause
                    )

                    is Message.UpdateTopAppBar -> {
                        updateTopAppBarState(message.update)
                    }
                }
            }
        }
    }

    private fun SavedStateHandle.registerSavedStateProvider() {
        setSavedStateProvider(KEY_SAVED_STATE) {
            val state = stateFlow.value
            Bundle().apply {
                putParcelable(KEY_WEB_AUTH_FLOW, state.webAuthFlow)
                putBoolean(KEY_FIRST_INIT, state.firstInit)
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

    fun handleOnCloseClick() {
        val pane = currentPane.value
        val topAppBarState = topAppBarState.value

        if (topAppBarState.error != null) {
            onCloseFromErrorClick(topAppBarState.error)
        } else if (pane.destination.closeWithoutConfirmation) {
            onCloseNoConfirmationClick(pane)
        } else {
            onCloseWithConfirmationClick(pane)
        }
    }

    private fun onUrlReceived(receivedUrl: String, status: String?) {
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
            val state = stateFlow.value
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

    private fun onCloseWithConfirmationClick(pane: Pane) = viewModelScope.launch {
        eventTracker.track(ClickNavBarClose(pane))
        navigationManager.tryNavigateTo(Destination.Exit(referrer = pane))
    }

    fun onBackClick(pane: Pane?) {
        viewModelScope.launch {
            pane?.let { eventTracker.track(ClickNavBarBack(pane)) }
        }
    }

    private fun onCloseNoConfirmationClick(pane: Pane) {
        viewModelScope.launch {
            eventTracker.track(ClickNavBarClose(pane))
        }
        closeAuthFlow(closeAuthFlowError = null)
    }

    fun onCloseFromErrorClick(error: Throwable) {
        // FinancialConnectionsError subclasses aren't public, so we just return their
        // backing StripeException.
        val exposedError = (error as? FinancialConnectionsError)?.stripeException ?: error
        closeAuthFlow(closeAuthFlowError = exposedError)
    }

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
            val state = stateFlow.value

            // prevents multiple complete triggers.
            if (state.completed) {
                return@launch
            }
            setState { copy(completed = true) }

            if (closeAuthFlowError is FinancialConnectionsAttestationError) {
                // Attestation error is a special case where we need to close the native flow
                // and continue with the AuthFlow on a web browser.
                finishWithResult(Failed(error = closeAuthFlowError))
                return@launch
            }

            runCatching {
                val completionResult = completeFinancialConnectionsSession(earlyTerminationCause, closeAuthFlowError)
                val session = completionResult.session

                eventTracker.track(
                    Complete(
                        pane = currentPane.value,
                        exception = null,
                        exceptionExtraMessage = null,
                        connectedAccounts = session.accounts.data.count(),
                        status = completionResult.status,
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
                        if (state.isLinkWithStripe) {
                            handleInstantDebitsCompletion(session)
                        } else {
                            handleFinancialConnectionsCompletion(session)
                        }
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
                        pane = currentPane.value,
                        exception = completeSessionError,
                        exceptionExtraMessage = errorMessage,
                        connectedAccounts = null,
                        status = "failed",
                    )
                )
                finishWithResult(Failed(closeAuthFlowError ?: completeSessionError))
            }
        }
    }

    private fun handleFinancialConnectionsCompletion(session: FinancialConnectionsSession) {
        FinancialConnections.emitEvent(
            name = Name.SUCCESS,
            metadata = Metadata(
                manualEntry = session.paymentAccount is BankAccount,
            )
        )

        val usesMicrodeposits = stateFlow.value.manualEntryUsesMicrodeposits
        val updatedSession = session.update(usesMicrodeposits = usesMicrodeposits)

        finishWithResult(
            Completed(
                financialConnectionsSession = updatedSession,
                token = updatedSession.parsedToken,
            )
        )
    }

    private suspend fun handleInstantDebitsCompletion(session: FinancialConnectionsSession) {
        val instantDebits = session.paymentAccount?.let { account ->
            createInstantDebitsResult(account.id)
        }

        val result = if (instantDebits != null) {
            Completed(
                instantDebits = instantDebits,
            )
        } else {
            Failed(
                error = UnclassifiedError(
                    name = "InstantDebitsCompletionError",
                    message = "Unable to complete Instant Debits flow due to missing PaymentAccount",
                ),
            )
        }

        finishWithResult(result)
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
        if (pane.destination.logPaneLaunched) {
            viewModelScope.launch {
                eventTracker.track(
                    PaneLaunched(
                        referrer = referrer,
                        pane = pane
                    )
                )
            }
        }
    }

    fun onBackgrounded() {
        trackBackgroundStateChanged(backgrounded = true)
    }

    fun onForegrounded() {
        trackBackgroundStateChanged(backgrounded = false)
    }

    private fun trackBackgroundStateChanged(backgrounded: Boolean) {
        val pane = currentPane.value
        eventTracker.track(
            AppBackgrounded(
                pane = pane,
                backgrounded = backgrounded,
            )
        )
    }

    override fun updateTopAppBarElevation(isElevated: Boolean) {
        topAppBarStateUpdatesByPane.updateWithNewEntry(currentPane.value) {
            it.copy(isContentScrolled = isElevated)
        }
    }

    private fun updateTopAppBarState(update: TopAppBarStateUpdate?) {
        if (update != null) {
            val updatedState = defaultTopAppBarState.apply(update)
            topAppBarStateUpdatesByPane.updateWithNewEntry(update.pane to updatedState)
        }
    }

    fun handlePaneChanged(pane: Pane) {
        currentPane.value = pane
    }

    companion object {

        private fun baseUrl(applicationId: String) =
            "stripe://auth-redirect/$applicationId"

        private const val PARAM_STATUS = "status"
        private const val PARAM_CODE = "code"
        private const val PARAM_ERROR_REASON = "error_reason"
        private const val STATUS_SUCCESS = "success"
        private const val STATUS_FAILURE = "failure"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle: SavedStateHandle = createSavedStateHandle()
                val app = this[APPLICATION_KEY] as Application
                // Arguments passed to the activity
                val args: FinancialConnectionsSheetNativeActivityArgs =
                    requireNotNull(getArgs(savedStateHandle))
                // If the ViewModel is recreated, it will be provided with the saved state.
                val savedState = savedStateHandle.get<Bundle>(KEY_SAVED_STATE)
                val state = FinancialConnectionsSheetNativeState(
                    args = args,
                    savedState = savedState
                )
                DaggerFinancialConnectionsSheetNativeComponent
                    .builder()
                    .initialSyncResponse(args.initialSyncResponse.takeIf { state.firstInit })
                    .application(app)
                    .configuration(state.configuration)
                    .sharedComponent(FinancialConnectionsSingletonSharedComponentHolder.getComponent(app))
                    .savedStateHandle(savedStateHandle)
                    .initialState(state)
                    .build()
                    .viewModel
            }
        }
    }

    // TODO avoid?
    override fun updateTopAppBar(state: FinancialConnectionsSheetNativeState): TopAppBarStateUpdate? {
        return null
    }
}

internal data class FinancialConnectionsSheetNativeState(
    val webAuthFlow: WebAuthFlowState,
    /**
     * Tracks whether this state was recreated from a process kill.
     */
    val firstInit: Boolean,
    val configuration: FinancialConnectionsSheet.Configuration,
    val reducedBranding: Boolean,
    val testMode: Boolean,
    val viewEffect: FinancialConnectionsSheetNativeViewEffect?,
    val completed: Boolean,
    val initialPane: Pane,
    val theme: Theme,
    val isLinkWithStripe: Boolean,
    val manualEntryUsesMicrodeposits: Boolean,
    val elementsSessionContext: ElementsSessionContext?,
) {

    /**
     * Used to build initial state based on args.
     */
    constructor(
        args: FinancialConnectionsSheetNativeActivityArgs,
        savedState: Bundle?
    ) : this(
        webAuthFlow = savedState?.getParcelable<WebAuthFlowState>(KEY_WEB_AUTH_FLOW)
            ?: WebAuthFlowState.Uninitialized,
        reducedBranding = args.initialSyncResponse.visual.reducedBranding,
        testMode = args.initialSyncResponse.manifest.livemode.not(),
        firstInit = savedState?.getBoolean(KEY_FIRST_INIT, true) ?: true,
        completed = false,
        initialPane = args.initialSyncResponse.manifest.nextPane,
        configuration = args.configuration,
        theme = args.initialSyncResponse.manifest.theme?.toLocalTheme() ?: Theme.default,
        viewEffect = null,
        isLinkWithStripe = args.initialSyncResponse.manifest.isLinkWithStripe ?: false,
        manualEntryUsesMicrodeposits = args.initialSyncResponse.manifest.manualEntryUsesMicrodeposits,
        elementsSessionContext = args.elementsSessionContext,
    )

    companion object {
        const val KEY_SAVED_STATE = "FinancialConnectionsSheetNativeState"
        const val KEY_WEB_AUTH_FLOW = "webAuthFlow"
        const val KEY_FIRST_INIT = "firstInit"
    }
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
    data object Uninitialized : WebAuthFlowState()

    /**
     * The web browser has been opened and the authentication flow is in progress.
     */
    @Parcelize
    data object InProgress : WebAuthFlowState()

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
    parentActivity().viewModel

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

private fun FinancialConnectionsSheetNativeState.toTopAppBarState(
    forceHideStripeLogo: Boolean,
): TopAppBarState {
    return TopAppBarState(
        hideStripeLogo = reducedBranding,
        forceHideStripeLogo = forceHideStripeLogo,
        isTestMode = testMode,
        theme = theme,
    )
}
