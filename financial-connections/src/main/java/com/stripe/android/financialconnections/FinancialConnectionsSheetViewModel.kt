package com.stripe.android.financialconnections

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnectionsSheetActivity.Companion.getArgs
import com.stripe.android.financialconnections.FinancialConnectionsSheetState.AuthFlowStatus
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.FinishWithResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenNativeAuthFlow
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ErrorCode
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Metadata
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEventReporter
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.browser.BrowserManager
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.di.DaggerFinancialConnectionsSheetComponent
import com.stripe.android.financialconnections.domain.FetchFinancialConnectionsSession
import com.stripe.android.financialconnections.domain.FetchFinancialConnectionsSessionForToken
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.GetOrFetchSync.RefetchCondition.Always
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowRouter
import com.stripe.android.financialconnections.exception.AppInitializationError
import com.stripe.android.financialconnections.exception.CustomManualEntryRequiredError
import com.stripe.android.financialconnections.features.manualentry.isCustomManualEntryError
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForData
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForInstantDebits
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForToken
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Canceled
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Completed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Failed
import com.stripe.android.financialconnections.launcher.InstantDebitsResult
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.utils.parcelable
import com.stripe.android.model.LinkMode
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Named

internal class FinancialConnectionsSheetViewModel @Inject constructor(
    @Named(APPLICATION_ID) private val applicationId: String,
    savedStateHandle: SavedStateHandle,
    private val getOrFetchSync: GetOrFetchSync,
    private val fetchFinancialConnectionsSession: FetchFinancialConnectionsSession,
    private val fetchFinancialConnectionsSessionForToken: FetchFinancialConnectionsSessionForToken,
    private val logger: Logger,
    private val browserManager: BrowserManager,
    private val eventReporter: FinancialConnectionsEventReporter,
    private val analyticsTracker: FinancialConnectionsAnalyticsTracker,
    private val nativeRouter: NativeAuthFlowRouter,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val initialState: FinancialConnectionsSheetState,
) : FinancialConnectionsViewModel<FinancialConnectionsSheetState>(initialState, nativeAuthFlowCoordinator) {

    private val mutex = Mutex()

    init {
        savedStateHandle.registerSavedStateProvider()
        if (initialState.initialArgs.isValid()) {
            eventReporter.onPresented(initialState.initialArgs.configuration)
            // avoid re-fetching manifest if already exists (this will happen on process recreations)
            if (initialState.manifest == null) fetchManifest()
        } else {
            val result = Failed(
                IllegalStateException("Invalid configuration provided when instantiating activity")
            )
            setState { copy(viewEffect = FinishWithResult(result)) }
        }
    }

    private fun SavedStateHandle.registerSavedStateProvider() =
        setSavedStateProvider(FinancialConnectionsSheetState.KEY_SAVED_STATE) {
            val state = stateFlow.value
            Bundle().apply {
                putParcelable(FinancialConnectionsSheetState.KEY_MANIFEST, state.manifest)
                putSerializable(FinancialConnectionsSheetState.KEY_WEB_AUTH_FLOW_STATUS, state.webAuthFlowStatus)
            }
        }

    /**
     * Fetches the [FinancialConnectionsSessionManifest] from the Stripe API to get the hosted auth flow URL
     * as well as the success and cancel callback URLs to verify.
     */
    private fun fetchManifest() {
        viewModelScope.launch {
            kotlin.runCatching {
                getOrFetchSync(refetchCondition = Always)
            }.onFailure {
                finishWithResult(stateFlow.value, Failed(it))
            }.onSuccess {
                openAuthFlow(it)
            }
        }
    }

    /**
     * Builds the ChromeCustomTab intent to launch the hosted auth flow and launches it.
     *
     * @param sync with manifest containing the hosted auth flow URL to launch
     *
     */
    private fun openAuthFlow(sync: SynchronizeSessionResponse) {
        if (browserManager.canOpenHttpsUrl().not()) {
            logNoBrowserAvailableAndFinish()
            return
        }
        val manifest = sync.manifest
        val isInstantDebits = stateFlow.value.isInstantDebits
        val nativeAuthFlowEnabled = nativeRouter.nativeAuthFlowEnabled(manifest)
        nativeRouter.logExposure(manifest)

        val linkMode = initialState.initialArgs.elementsSessionContext?.linkMode
        val hostedAuthUrl = buildHostedAuthUrl(
            hostedAuthUrl = manifest.hostedAuthUrl,
            isInstantDebits = isInstantDebits,
            linkMode = linkMode,
        )
        if (hostedAuthUrl == null) {
            finishWithResult(
                state = stateFlow.value,
                result = Failed(IllegalArgumentException("hostedAuthUrl is required!"))
            )
        } else {
            FinancialConnections.emitEvent(name = Name.OPEN)
            if (nativeAuthFlowEnabled) {
                setState {
                    copy(
                        manifest = manifest,
                        webAuthFlowStatus = AuthFlowStatus.NONE,
                        viewEffect = OpenNativeAuthFlow(
                            configuration = initialArgs.configuration,
                            initialSyncResponse = sync,
                            elementsSessionContext = initialArgs.elementsSessionContext,
                        )
                    )
                }
            } else {
                FinancialConnections.emitEvent(name = Name.FLOW_LAUNCHED_IN_BROWSER)
                setState {
                    copy(
                        manifest = manifest,
                        webAuthFlowStatus = AuthFlowStatus.ON_EXTERNAL_ACTIVITY,
                        viewEffect = OpenAuthFlowWithUrl(hostedAuthUrl)
                    )
                }
            }
        }
    }

    private fun buildHostedAuthUrl(
        hostedAuthUrl: String?,
        isInstantDebits: Boolean,
        linkMode: LinkMode?,
    ): String? {
        if (hostedAuthUrl == null) {
            return null
        }

        val queryParams = mutableListOf(hostedAuthUrl)
        if (isInstantDebits) {
            // For Instant Debits, add a query parameter to the hosted auth URL so that payment account creation
            // takes place on the web side of the flow and the payment method ID is returned to the app.
            queryParams.add("return_payment_method=true")
            linkMode?.let { queryParams.add("link_mode=${it.value}") }
        }

        return queryParams.joinToString("&")
    }

    private fun logNoBrowserAvailableAndFinish() {
        val error = AppInitializationError("No Web browser available to launch AuthFlow")
        analyticsTracker.logError(
            "error Launching the Auth Flow",
            logger = logger,
            pane = Pane.UNEXPECTED_ERROR,
            error = error
        )
        finishWithResult(
            state = stateFlow.value,
            result = Failed(error)
        )
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
    internal fun onActivityRecreated() {
        setState {
            copy(
                activityRecreated = true
            )
        }
    }

    fun onDismissed() {
        finishWithResult(
            state = stateFlow.value,
            result = Canceled
        )
    }

    /**
     *  If activity resumes and we did not receive a callback from the custom tabs,
     *  then the user hit the back button or closed the custom tabs UI, so return result as
     *  canceled.
     */
    internal fun onResume() {
        viewModelScope.launch {
            mutex.withLock {
                val state = stateFlow.value
                if (state.activityRecreated.not()) {
                    when (state.webAuthFlowStatus) {
                        AuthFlowStatus.ON_EXTERNAL_ACTIVITY -> finishWithResult(
                            state = state,
                            result = Canceled
                        )

                        AuthFlowStatus.INTERMEDIATE_DEEPLINK -> setState {
                            copy(
                                webAuthFlowStatus = AuthFlowStatus.ON_EXTERNAL_ACTIVITY
                            )
                        }

                        AuthFlowStatus.NONE -> Unit
                    }
                }
            }
        }
    }

    /**
     * If activity receives result and we did not receive a callback from the custom tabs,
     * if activity got recreated and the auth flow is still active then the user hit
     * the back button or closed the custom tabs UI, so return result as canceled.
     */
    internal fun onBrowserActivityResult() {
        viewModelScope.launch {
            mutex.withLock {
                val state = stateFlow.value
                if (state.activityRecreated) {
                    when (state.webAuthFlowStatus) {
                        AuthFlowStatus.ON_EXTERNAL_ACTIVITY -> finishWithResult(
                            state = state,
                            result = Canceled
                        )

                        AuthFlowStatus.INTERMEDIATE_DEEPLINK -> setState {
                            copy(
                                webAuthFlowStatus = AuthFlowStatus.ON_EXTERNAL_ACTIVITY
                            )
                        }

                        AuthFlowStatus.NONE -> Unit
                    }
                }
            }
        }
    }

    internal fun onNativeAuthFlowResult(activityResult: ActivityResult) {
        val result: FinancialConnectionsSheetActivityResult? = activityResult.data
            ?.parcelable(FinancialConnectionsSheetNativeActivity.EXTRA_RESULT)
        if (activityResult.resultCode == Activity.RESULT_OK && result != null) {
            finishWithResult(stateFlow.value, result, fromNative = true)
        } else {
            finishWithResult(stateFlow.value, Canceled, fromNative = true)
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
                finishWithResult(
                    state = state,
                    result = Completed(financialConnectionsSession = it)
                )
            }.onFailure { error ->
                finishWithResult(stateFlow.value, Failed(error))
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
                finishWithResult(
                    state = state,
                    result = Completed(financialConnectionsSession = las, token = token)
                )
            }.onFailure { error ->
                finishWithResult(stateFlow.value, Failed(error))
            }
        }
    }

    /**
     * If a user cancels the hosted auth flow either by closing the custom tab with the back button
     * or clicking a cancel link within the hosted auth flow and the activity received the canceled
     * URL callback, fetch the current session to check its status, and notify
     * the [FinancialConnectionsSheetResultCallback] with the corresponding result.
     */
    private fun onUserCancel(state: FinancialConnectionsSheetState) {
        viewModelScope.launch {
            kotlin.runCatching {
                fetchFinancialConnectionsSession(clientSecret = state.sessionSecret)
            }.onSuccess { session ->
                finishWithResult(
                    state = state,
                    result = if (session.isCustomManualEntryError()) {
                        Failed(CustomManualEntryRequiredError())
                    } else {
                        Canceled
                    }
                )
            }.onFailure { error ->
                finishWithResult(stateFlow.value, Failed(error))
            }
        }
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
        viewModelScope.launch {
            mutex.withLock {
                val receivedUrl: Uri? = intent?.data?.toString()?.toUriOrNull()
                val state = stateFlow.value
                when {
                    // stripe-auth://native-redirect
                    receivedUrl?.host == "native-redirect" ->
                        onStartApp2App(
                            receivedUrl.toString()
                                .replaceFirst("stripe-auth://native-redirect/$applicationId/", "")
                        )

                    // stripe-auth://link-accounts/.../authentication_return
                    (receivedUrl?.host == "link-accounts") &&
                        (
                            receivedUrl.buildUpon()?.clearQuery()
                                ?.build()?.path == "/$applicationId/authentication_return"
                            ) ->
                        onFinishApp2App(receivedUrl)

                    // stripe-auth://link-accounts/{applicationId/success
                    receivedUrl?.buildUpon()?.clearQuery()
                        .toString() == state.manifest?.successUrl -> onFlowSuccess(
                        state,
                        receivedUrl
                    )

                    // stripe-auth://link-accounts/{applicationId/cancel
                    receivedUrl?.buildUpon()?.clearQuery()
                        .toString() == state.manifest?.cancelUrl -> onFlowCancelled(state)

                    else -> {
                        setState { copy(webAuthFlowStatus = AuthFlowStatus.NONE) }
                        finishWithResult(
                            state,
                            Failed(Exception("Error processing FinancialConnectionsSheet intent"))
                        )
                    }
                }
            }
        }
    }

    private fun onStartApp2App(unwrappedUriString: String) {
        setState {
            copy(
                webAuthFlowStatus = AuthFlowStatus.INTERMEDIATE_DEEPLINK,
                activityRecreated = false,
                viewEffect = OpenAuthFlowWithUrl(unwrappedUriString)
            )
        }
    }

    private fun onFinishApp2App(receivedUrl: Uri) {
        setState {
            val authFlowResumeUrl =
                "${manifest!!.hostedAuthUrl}&startPolling=true&${receivedUrl.fragment}"
            copy(
                webAuthFlowStatus = AuthFlowStatus.INTERMEDIATE_DEEPLINK,
                activityRecreated = false,
                viewEffect = OpenAuthFlowWithUrl(authFlowResumeUrl)
            )
        }
    }

    private fun onFlowSuccess(state: FinancialConnectionsSheetState, receivedUrl: Uri?) {
        if (receivedUrl == null) {
            finishWithResult(
                state = state,
                result = Failed(Exception("Intent url received from web flow is null"))
            )
        } else {
            setState { copy(webAuthFlowStatus = AuthFlowStatus.NONE) }
            when (state.initialArgs) {
                is ForData -> fetchFinancialConnectionsSession(state)
                is ForToken -> fetchFinancialConnectionsSessionForToken(state)
                is ForInstantDebits -> onSuccessFromInstantDebits(receivedUrl)
            }
        }
    }

    private fun onSuccessFromInstantDebits(url: Uri) {
        runCatching { requireNotNull(url.getQueryParameter(QUERY_PARAM_PAYMENT_METHOD_ID)) }
            .onSuccess { paymentMethodId ->
                withState {
                    finishWithResult(
                        state = it,
                        result = Completed(
                            instantDebits = InstantDebitsResult(
                                paymentMethodId = paymentMethodId,
                                last4 = url.getQueryParameter(QUERY_PARAM_LAST4),
                                bankName = url.getQueryParameter(QUERY_BANK_NAME)
                            ),
                            financialConnectionsSession = null,
                            token = null
                        )
                    )
                }
            }
            .onFailure { error ->
                logger.error("Could not retrieve payment method parameters from success url", error)
                finishWithResult(stateFlow.value, Failed(error))
            }
    }

    private fun onFlowCancelled(state: FinancialConnectionsSheetState) {
        setState { copy(webAuthFlowStatus = AuthFlowStatus.NONE) }
        onUserCancel(state)
    }

    internal fun onViewEffectLaunched() {
        setState { copy(viewEffect = null) }
    }

    private fun String.toUriOrNull(): Uri? {
        Uri.parse(this).buildUpon().clearQuery()
        return kotlin.runCatching {
            return Uri.parse(this)
        }.onFailure {
            logger.error("Could not parse web flow url", it)
        }.getOrNull()
    }

    private fun finishWithResult(
        state: FinancialConnectionsSheetState,
        result: FinancialConnectionsSheetActivityResult,
        fromNative: Boolean = false,
        @StringRes finishMessage: Int? = null,
    ) {
        eventReporter.onResult(state.initialArgs.configuration, result)
        // Native emits its own events before finishing.
        if (fromNative.not()) {
            when (result) {
                is Completed -> FinancialConnections.emitEvent(Name.SUCCESS)
                is Canceled -> FinancialConnections.emitEvent(Name.CANCEL)
                is Failed -> FinancialConnections.emitEvent(
                    name = Name.ERROR,
                    metadata = Metadata(errorCode = ErrorCode.UNEXPECTED_ERROR)
                )
            }
        }
        setState { copy(viewEffect = FinishWithResult(result, finishMessage)) }
    }

    companion object {

        val Factory = viewModelFactory {
            initializer {
                val savedStateHandle: SavedStateHandle = createSavedStateHandle()
                // If the ViewModel is recreated, it will be provided with the saved state.
                val savedState = savedStateHandle.get<Bundle>(FinancialConnectionsSheetState.KEY_SAVED_STATE)
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                // Arguments passed to the activity
                val args: FinancialConnectionsSheetActivityArgs = requireNotNull(getArgs(savedStateHandle))
                val state = FinancialConnectionsSheetState(args, savedState)
                DaggerFinancialConnectionsSheetComponent
                    .builder()
                    .application(app)
                    .savedStateHandle(savedStateHandle)
                    .initialState(state)
                    .configuration(state.initialArgs.configuration)
                    .build().viewModel
            }
        }

        internal const val MAX_ACCOUNTS = 100
        internal const val QUERY_PARAM_PAYMENT_METHOD_ID = "payment_method_id"
        internal const val QUERY_PARAM_LAST4 = "last4"
        internal const val QUERY_BANK_NAME = "bank_name"
    }

    override fun updateTopAppBar(state: FinancialConnectionsSheetState): TopAppBarStateUpdate? {
        return null
    }
}
