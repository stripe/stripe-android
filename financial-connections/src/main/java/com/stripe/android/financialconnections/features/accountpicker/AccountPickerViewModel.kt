package com.stripe.android.financialconnections.features.accountpicker

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.RequestNextStep
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionAccounts
import com.stripe.android.financialconnections.domain.SelectAccounts
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

internal class AccountPickerViewModel @Inject constructor(
    initialState: AccountPickerState,
    private val selectAccounts: SelectAccounts,
    private val getManifest: GetManifest,
    private val coordinator: NativeAuthFlowCoordinator,
    private val pollAuthorizationSessionAccounts: PollAuthorizationSessionAccounts
) : MavericksViewModel<AccountPickerState>(initialState) {

    init {
        suspend {
            val authSession = requireNotNull(getManifest().activeAuthSession)
            pollAuthorizationSessionAccounts(authSession.id)
        }.execute { copy(accounts = it) }
    }

    fun onAccountClicked(account: PartnerAccount) {
        withState {
            if (it.selectedIds.contains(account.id)) {
                setState { copy(selectedIds = selectedIds - account.id) }
            } else {
                setState { copy(selectedIds = selectedIds + account.id) }
            }
        }
    }

    fun selectAccounts() {
        withState { state ->
            state.accounts()?.let { accounts ->
                suspend {
                    val manifest = getManifest()
                    val accountsList: PartnerAccountsList = selectAccounts(
                        selectedAccounts = accounts.data.filter { state.selectedIds.contains(it.id) },
                        sessionId = manifest.activeAuthSession!!.id,
                    )
                    coordinator().emit(RequestNextStep(currentStep = NavigationDirections.accountPicker))
                    accountsList
                }.execute {
                    copy(selectAccounts = it)
                }
            }
        }
    }

    companion object :
        MavericksViewModelFactory<AccountPickerViewModel, AccountPickerState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: AccountPickerState
        ): AccountPickerViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .accountPickerBuilder
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class AccountPickerState(
    val accounts: Async<PartnerAccountsList> = Uninitialized,
    val selectAccounts: Async<PartnerAccountsList> = Uninitialized,
    val selectedIds: Set<String> = emptySet()
) : MavericksState {

    val isLoading: Boolean
        get() = selectAccounts is Loading || accounts is Loading
}
