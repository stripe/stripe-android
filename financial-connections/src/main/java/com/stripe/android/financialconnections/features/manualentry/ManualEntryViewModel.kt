package com.stripe.android.financialconnections.features.manualentry

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Complete
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Complete.EarlyTerminationCause.USER_INITIATED_WITH_CUSTOM_MANUAL_ENTRY
import com.stripe.android.financialconnections.domain.PollAttachPaymentAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.ManualEntryMode
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.navigation.Destination.ManualEntrySuccess
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

internal class ManualEntryViewModel @Inject constructor(
    initialState: ManualEntryState,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val pollAttachPaymentAccount: PollAttachPaymentAccount,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getOrFetchSync: GetOrFetchSync,
    private val navigationManager: NavigationManager,
    private val logger: Logger
) : MavericksViewModel<ManualEntryState>(initialState) {

    init {
        observeAsyncs()
        suspend {
            val sync = getOrFetchSync()
            val manifest = requireNotNull(sync.manifest)
            eventTracker.track(PaneLoaded(Pane.MANUAL_ENTRY))
            ManualEntryState.Payload(
                verifyWithMicrodeposits = manifest.manualEntryUsesMicrodeposits,
                customManualEntry = manifest.manualEntryMode == ManualEntryMode.CUSTOM,
                testMode = manifest.livemode.not()
            )
        }.execute {
            copy(payload = it)
        }
    }

    private fun observeAsyncs() {
        onAsync(
            ManualEntryState::payload,
            onSuccess = { payload ->
                if (payload.customManualEntry) {
                    nativeAuthFlowCoordinator().emit(
                        Complete(
                            USER_INITIATED_WITH_CUSTOM_MANUAL_ENTRY
                        )
                    )
                }
            },
        )
        onAsync(
            ManualEntryState::linkPaymentAccount,
            onFail = {
                eventTracker.logError(
                    extraMessage = "Error linking payment account",
                    error = it,
                    logger = logger,
                    pane = PANE
                )
            },
        )
    }

    fun onRoutingEntered(input: String) {
        setState { copy(routing = input.filter { it.isDigit() }) }
        withState {
            val error = ManualEntryInputValidator.getRoutingErrorIdOrNull(input)
            setState { copy(routingError = error) }
        }
    }

    fun onAccountEntered(input: String) {
        setState { copy(account = input.filter { it.isDigit() }) }
        withState {
            val error = ManualEntryInputValidator.getAccountErrorIdOrNull(input)
            setState { copy(accountError = error) }
        }
    }

    fun onAccountConfirmEntered(input: String) {
        setState { copy(accountConfirm = input.filter { it.isDigit() }) }
        withState {
            val error: Int? = ManualEntryInputValidator.getAccountConfirmIdOrNull(
                accountInput = it.account,
                accountConfirmInput = input
            )
            setState { copy(accountConfirmError = error) }
        }
    }

    fun onSubmit() {
        suspend {
            val state = awaitState()
            val sync = getOrFetchSync()
            pollAttachPaymentAccount(
                sync = sync,
                activeInstitution = null,
                consumerSessionClientSecret = null,
                params = PaymentAccountParams.BankAccount(
                    routingNumber = requireNotNull(state.routing),
                    accountNumber = requireNotNull(state.account)
                )
            ).also {
                if (sync.manifest.manualEntryUsesMicrodeposits) {
                    navigationManager.tryNavigateTo(
                        ManualEntrySuccess(
                            referrer = PANE,
                            args = ManualEntrySuccess.argMap(
                                microdepositVerificationMethod = it.microdepositVerificationMethod,
                                last4 = state.account.takeLast(4)
                            )
                        )
                    )
                } else {
                    nativeAuthFlowCoordinator().emit(Complete())
                }
            }
        }.execute { copy(linkPaymentAccount = it) }
    }

    fun onTestFill() {
        setState {
            copy(
                routing = "110000000",
                account = "000123456789",
                accountConfirm = "000123456789",
                routingError = null,
                accountError = null,
                accountConfirmError = null
            )
        }
    }

    companion object :
        MavericksViewModelFactory<ManualEntryViewModel, ManualEntryState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: ManualEntryState
        ): ManualEntryViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .manualEntryBuilder
                .initialState(state)
                .build()
                .viewModel
        }

        private val PANE = Pane.MANUAL_ENTRY
    }
}

internal data class ManualEntryState(
    val payload: Async<Payload> = Uninitialized,
    val routing: String = "",
    val account: String = "",
    val accountConfirm: String = "",
    val routingError: Int? = null,
    val accountError: Int? = null,
    val accountConfirmError: Int? = null,
    val linkPaymentAccount: Async<LinkAccountSessionPaymentAccount> = Uninitialized
) : MavericksState {

    data class Payload(
        val verifyWithMicrodeposits: Boolean,
        val customManualEntry: Boolean,
        val testMode: Boolean
    )

    val isValidForm
        get() =
            (routing to routingError).valid() &&
                (account to accountError).valid() &&
                (accountConfirm to accountConfirmError).valid()

    private fun Pair<String, Int?>.valid() = first.isNotEmpty() && second == null
}
