package com.stripe.android.financialconnections.features.manualentry

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PaneLoaded
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.PollAttachPaymentAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.ClientPane
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

@Suppress("LongParameterList")
internal class ManualEntryViewModel @Inject constructor(
    initialState: ManualEntryState,
    private val pollAttachPaymentAccount: PollAttachPaymentAccount,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getManifest: GetManifest,
    private val goNext: GoNext,
    private val logger: Logger
) : MavericksViewModel<ManualEntryState>(initialState) {

    init {
        logErrors()
        observeInputs()
        suspend {
            getManifest().manualEntryUsesMicrodeposits.also {
                eventTracker.track(PaneLoaded(ClientPane.MANUAL_ENTRY))
            }
        }.execute {
            copy(verifyWithMicrodeposits = it() ?: false)
        }
    }

    private fun observeInputs() {
        onEach(ManualEntryState::accountConfirm) { input ->
            if (input != null) withState {
                val error: Int? = ManualEntryInputValidator.getAccountConfirmIdOrNull(
                    accountInput = it.account ?: "",
                    accountConfirmInput = input
                )
                setState { copy(accountConfirmError = error) }
            }
        }
        onEach(ManualEntryState::account) { input ->
            if (input != null) setState {
                val error = ManualEntryInputValidator.getAccountErrorIdOrNull(input)
                copy(accountError = error)
            }
        }
        onEach(ManualEntryState::routing) { input ->
            if (input != null) setState {
                val error = ManualEntryInputValidator.getRoutingErrorIdOrNull(input)
                copy(routingError = error)
            }
        }
    }

    private fun logErrors() {
        onAsync(
            ManualEntryState::linkPaymentAccount,
            onFail = {
                logger.error("Error linking payment account", it)
                eventTracker.track(FinancialConnectionsEvent.Error(ClientPane.MANUAL_ENTRY, it))
            },
        )
    }

    fun onRoutingEntered(input: String) {
        val filteredInput = input.filter { it.isDigit() }
        setState { copy(routing = filteredInput) }
    }

    fun onAccountEntered(input: String) {
        val filteredInput = input.filter { it.isDigit() }
        setState { copy(account = filteredInput) }
    }

    fun onAccountConfirmEntered(input: String) {
        val filteredInput = input.filter { it.isDigit() }
        setState {
            copy(accountConfirm = filteredInput)
        }
    }

    @Suppress("MagicNumber")
    fun onSubmit() {
        suspend {
            val state = awaitState()
            val manifest = getManifest()
            pollAttachPaymentAccount(
                allowManualEntry = manifest.allowManualEntry,
                activeInstitution = null,
                params = PaymentAccountParams.BankAccount(
                    routingNumber = requireNotNull(state.routing),
                    accountNumber = requireNotNull(state.account)
                )
            ).also {
                goNext(
                    it.nextPane ?: ClientPane.MANUAL_ENTRY_SUCCESS,
                    args = NavigationDirections.ManualEntrySuccess.argMap(
                        microdepositVerificationMethod = it.microdepositVerificationMethod,
                        last4 = state.account.takeLast(4)
                    )
                )
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
    }
}

internal data class ManualEntryState(
    val routing: String? = null,
    val account: String? = null,
    val accountConfirm: String? = null,
    val routingError: Int? = null,
    val accountError: Int? = null,
    val accountConfirmError: Int? = null,
    val linkPaymentAccount: Async<LinkAccountSessionPaymentAccount> = Uninitialized,
    val verifyWithMicrodeposits: Boolean = false
) : MavericksState {

    val isValidForm
        get() =
            (routing to routingError).valid() &&
                (account to accountError).valid() &&
                (accountConfirm to accountConfirmError).valid()

    private fun Pair<String?, Int?>.valid() = first != null && second == null
}
