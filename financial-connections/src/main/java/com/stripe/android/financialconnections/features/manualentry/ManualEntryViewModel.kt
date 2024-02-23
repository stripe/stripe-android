package com.stripe.android.financialconnections.features.manualentry

import androidx.compose.ui.text.input.TextFieldValue
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
import com.stripe.android.financialconnections.features.manualentry.ManualEntryInputValidator.ACCOUNT_NUMBER_MAX_LENGTH
import com.stripe.android.financialconnections.features.manualentry.ManualEntryInputValidator.ROUTING_NUMBER_LENGTH
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.ManualEntryMode
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.navigation.Destination.ManualEntrySuccess
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.ui.components.filter
import com.stripe.android.financialconnections.ui.components.take
import javax.inject.Inject

@Suppress("LongParameterList")
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
        observeInputs()
        suspend {
            val sync = getOrFetchSync()
            val manifest = requireNotNull(sync.manifest)
            eventTracker.track(PaneLoaded(Pane.MANUAL_ENTRY))
            ManualEntryState.Payload(
                verifyWithMicrodeposits = manifest.manualEntryUsesMicrodeposits,
                customManualEntry = manifest.manualEntryMode == ManualEntryMode.CUSTOM
            )
        }.execute {
            copy(payload = it)
        }
    }

    private fun observeInputs() {
        onEach(ManualEntryState::accountConfirm) { input ->
            if (input.value != null) {
                setState {
                    val error: Int? = ManualEntryInputValidator.getAccountConfirmIdOrNull(
                        accountInput = account.value.orEmpty(),
                        accountConfirmInput = input.value,
                    )
                    copy(accountConfirm = accountConfirm.copy(error = error))
                }
            }
        }
        onEach(ManualEntryState::account) { input ->
            if (input.value != null) {
                setState {
                    val error = ManualEntryInputValidator.getAccountErrorIdOrNull(input.value)
                    val confirmError: Int? = ManualEntryInputValidator.getAccountConfirmIdOrNull(
                        accountInput = input.value,
                        accountConfirmInput = accountConfirm.value.orEmpty(),
                    ).takeIf {
                        // Clear a potentially stale confirm error
                        accountConfirm.error != null
                    }
                    copy(
                        account = account.copy(error = error),
                        accountConfirm = accountConfirm.copy(error = confirmError),
                    )
                }
            }
        }
        onEach(ManualEntryState::routing) { input ->
            if (input.value != null) {
                setState {
                    val error = ManualEntryInputValidator.getRoutingErrorIdOrNull(input.value)
                    copy(routing = routing.copy(error = error))
                }
            }
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
        val filteredInput = input.filter { it.isDigit() }
        setState { copy(routing = routing.copy(value = filteredInput)) }
    }

    fun onAccountEntered(input: String) {
        val filteredInput = input.filter { it.isDigit() }
        setState { copy(account = account.copy(value = filteredInput)) }
    }

    fun onAccountConfirmEntered(input: String) {
        val filteredInput = input.filter { it.isDigit() }
        setState {
            copy(accountConfirm = accountConfirm.copy(value = filteredInput))
        }
    }

    @Suppress("MagicNumber")
    fun onSubmit() {
        suspend {
            val state = awaitState()
            val sync = getOrFetchSync()
            pollAttachPaymentAccount(
                sync = sync,
                activeInstitution = null,
                consumerSessionClientSecret = null,
                params = PaymentAccountParams.BankAccount(
                    routingNumber = requireNotNull(state.routing.value),
                    accountNumber = requireNotNull(state.account.value),
                )
            ).also {
                if (sync.manifest.manualEntryUsesMicrodeposits) {
                    navigationManager.tryNavigateTo(
                        ManualEntrySuccess(
                            referrer = PANE,
                            args = ManualEntrySuccess.argMap(
                                microdepositVerificationMethod = it.microdepositVerificationMethod,
                                last4 = state.account.value.takeLast(4),
                            )
                        )
                    )
                } else {
                    nativeAuthFlowCoordinator().emit(Complete())
                }
            }
        }.execute { copy(linkPaymentAccount = it) }
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
    val routing: InputState = InputState(maxLength = ROUTING_NUMBER_LENGTH),
    val account: InputState = InputState(maxLength = ACCOUNT_NUMBER_MAX_LENGTH),
    val accountConfirm: InputState = InputState(maxLength = ACCOUNT_NUMBER_MAX_LENGTH),
    val linkPaymentAccount: Async<LinkAccountSessionPaymentAccount> = Uninitialized,
) : MavericksState {

    data class InputState(
        val maxLength: Int,
        val value: String? = null,
        val error: Int? = null,
    ) {

        fun filter(fieldValue: TextFieldValue): TextFieldValue {
            return fieldValue.filter { it.isDigit() }.take(maxLength)
        }
    }

    data class Payload(
        val verifyWithMicrodeposits: Boolean,
        val customManualEntry: Boolean
    )

    val isValidForm: Boolean
        get() = routing.valid() && account.valid() && accountConfirm.valid()

    private fun InputState.valid() = value != null && error == null
}
