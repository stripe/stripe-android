package com.stripe.android.financialconnections.features.manualentry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    // Keep form fields outside of State for immediate updates.
    private var _routing: String? by mutableStateOf(null)
    private var _account: String? by mutableStateOf(null)
    private var _accountConfirm: String? by mutableStateOf(null)

    val routing: String get() = _routing ?: ""
    val account: String get() = _account ?: ""
    val accountConfirm: String get() = _accountConfirm ?: ""

    val form: StateFlow<ManualEntryFormState> = combine(
        snapshotFlow { _routing },
        snapshotFlow { _account },
        snapshotFlow { _accountConfirm },
        ::ManualEntryFormState
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ManualEntryFormState(
            routing = null,
            account = null,
            accountConfirm = null,
        )
    )

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
        _routing = input.filter { it.isDigit() }
    }

    fun onAccountEntered(input: String) {
        _account = input.filter { it.isDigit() }
    }

    fun onAccountConfirmEntered(input: String) {
        _accountConfirm = input.filter { it.isDigit() }
    }

    fun onSubmit() {
        suspend {
            val sync = getOrFetchSync()
            pollAttachPaymentAccount(
                sync = sync,
                activeInstitution = null,
                consumerSessionClientSecret = null,
                params = PaymentAccountParams.BankAccount(
                    routingNumber = requireNotNull(routing),
                    accountNumber = requireNotNull(account)
                )
            ).also {
                if (sync.manifest.manualEntryUsesMicrodeposits) {
                    navigationManager.tryNavigateTo(
                        ManualEntrySuccess(
                            referrer = PANE,
                            args = ManualEntrySuccess.argMap(
                                microdepositVerificationMethod = it.microdepositVerificationMethod,
                                last4 = requireNotNull(account).takeLast(4)
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
        _routing = "110000000"
        _account = "000123456789"
        _accountConfirm = "000123456789"
        onSubmit()
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
    val linkPaymentAccount: Async<LinkAccountSessionPaymentAccount> = Uninitialized
) : MavericksState {

    data class Payload(
        val verifyWithMicrodeposits: Boolean,
        val customManualEntry: Boolean,
        val testMode: Boolean
    )
}
