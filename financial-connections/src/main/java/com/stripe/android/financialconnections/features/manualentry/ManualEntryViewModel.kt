package com.stripe.android.financialconnections.features.manualentry

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.domain.AttachPaymentAccount
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.NextPane
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

@Suppress("LongParameterList")
internal class ManualEntryViewModel @Inject constructor(
    initialState: ManualEntryState,
    val attachPaymentAccount: AttachPaymentAccount,
    val getManifest: GetManifest,
    val goNext: GoNext,
    val logger: Logger
) : MavericksViewModel<ManualEntryState>(initialState) {

    init {
        logErrors()
        suspend {
            getManifest().manualEntryUsesMicrodeposits
        }.execute {
            copy(verifyWithMicrodeposits = it() ?: false)
        }
    }

    private fun logErrors() {
        onAsync(ManualEntryState::linkPaymentAccount, onFail = {
            logger.error("Error linking payment account", it)
        })
    }

    fun onRoutingEntered(input: String) {
        val filteredInput = input.filter { it.isDigit() }
        setState {
            copy(
                routing = filteredInput to
                    ManualEntryInputValidator.getRoutingErrorIdOrNull(filteredInput),
            )
        }
    }

    fun onAccountEntered(input: String) {
        val filteredInput = input.filter { it.isDigit() }
        setState {
            copy(
                account = filteredInput to
                    ManualEntryInputValidator.getAccountErrorIdOrNull(filteredInput)
            )
        }
    }

    fun onAccountConfirmEntered(input: String) {
        val filteredInput = input.filter { it.isDigit() }
        setState {
            copy(
                accountConfirm = filteredInput to
                    ManualEntryInputValidator.getAccountConfirmIdOrNull(
                        accountInput = account.first ?: "",
                        accountConfirmInput = filteredInput
                    )
            )
        }
    }

    @Suppress("MagicNumber")
    fun onSubmit() {
        suspend {
            val state = awaitState()
            attachPaymentAccount(
                PaymentAccountParams.BankAccount(
                    routingNumber = requireNotNull(state.routing.first),
                    accountNumber = requireNotNull(state.account.first),
                )
            ).also {
                goNext(
                    it.nextPane ?: NextPane.MANUAL_ENTRY_SUCCESS,
                    args = NavigationDirections.ManualEntrySuccess.argMap(
                        microdepositVerificationMethod = it.microdepositVerificationMethod,
                        last4 = state.account.first?.takeLast(4)
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
    val routing: Pair<String?, Int?> = null to null,
    val account: Pair<String?, Int?> = null to null,
    val accountConfirm: Pair<String?, Int?> = null to null,
    val linkPaymentAccount: Async<LinkAccountSessionPaymentAccount> = Uninitialized,
    val verifyWithMicrodeposits: Boolean = false
) : MavericksState {

    val isValidForm
        get() = routing.valid() && account.valid() && accountConfirm.valid()

    private fun Pair<String?, Int?>.valid() = first != null && second == null
}
