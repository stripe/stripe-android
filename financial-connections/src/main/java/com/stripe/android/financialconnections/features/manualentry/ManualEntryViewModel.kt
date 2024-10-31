package com.stripe.android.financialconnections.features.manualentry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Complete
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.Complete.EarlyTerminationCause.USER_INITIATED_WITH_CUSTOM_MANUAL_ENTRY
import com.stripe.android.financialconnections.domain.PollAttachPaymentAccount
import com.stripe.android.financialconnections.domain.UpdateCachedAccounts
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.ManualEntryMode
import com.stripe.android.financialconnections.model.ManualEntryPane
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.repository.SuccessContentRepository
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.utils.error
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal class ManualEntryViewModel @AssistedInject constructor(
    @Assisted initialState: ManualEntryState,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val pollAttachPaymentAccount: PollAttachPaymentAccount,
    private val successContentRepository: SuccessContentRepository,
    private val updateCachedAccounts: UpdateCachedAccounts,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getOrFetchSync: GetOrFetchSync,
    private val navigationManager: NavigationManager,
    private val logger: Logger
) : FinancialConnectionsViewModel<ManualEntryState>(initialState, nativeAuthFlowCoordinator) {

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
            val content = requireNotNull(manifest.displayText?.manualEntryPane)
            eventTracker.track(PaneLoaded(Pane.MANUAL_ENTRY))
            ManualEntryState.Payload(
                customManualEntry = manifest.manualEntryMode == ManualEntryMode.CUSTOM,
                content = content,
            )
        }.execute {
            copy(payload = it)
        }
    }

    override fun updateTopAppBar(state: ManualEntryState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = PANE,
            allowBackNavigation = true,
            error = state.payload.error,
        )
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
                params = PaymentAccountParams.BankAccount(
                    routingNumber = routing,
                    accountNumber = account
                )
            ).also {
                clearCachedAccounts()
                if (sync.manifest.manualEntryUsesMicrodeposits) {
                    successContentRepository.set(
                        heading = TextResource.StringId(
                            R.string.stripe_success_pane_title_microdeposits
                        ),
                        message = TextResource.StringId(
                            R.string.stripe_success_pane_desc_microdeposits,
                            listOf(account.takeLast(4))
                        )
                    )
                }
                val nextPane = (it.nextPane ?: Pane.MANUAL_ENTRY_SUCCESS).destination(referrer = PANE)
                navigationManager.tryNavigateTo(nextPane)
            }
        }.execute { copy(linkPaymentAccount = it) }
    }

    // Keeping accounts selected can lead to them being passed along
    // to the Link signup/save call later in the flow. We don't need them anymore since we know
    // they've failed us in some way at this point.
    private suspend fun clearCachedAccounts() {
        runCatching { updateCachedAccounts(emptyList()) }
    }

    fun onTestFill() {
        _routing = "110000000"
        _account = "000123456789"
        _accountConfirm = "000123456789"
        onSubmit()
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: ManualEntryState): ManualEntryViewModel
    }

    companion object {

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent.manualEntryViewModelFactory.create(ManualEntryState())
                }
            }

        private val PANE = Pane.MANUAL_ENTRY
    }
}

internal data class ManualEntryState(
    val payload: Async<Payload> = Uninitialized,
    val linkPaymentAccount: Async<LinkAccountSessionPaymentAccount> = Uninitialized
) {

    data class Payload(
        val content: ManualEntryPane,
        val customManualEntry: Boolean,
    )
}
