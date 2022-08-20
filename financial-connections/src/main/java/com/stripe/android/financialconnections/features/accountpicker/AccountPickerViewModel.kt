package com.stripe.android.financialconnections.features.accountpicker

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.RequestNextStep
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionAccounts
import com.stripe.android.financialconnections.domain.SelectAccounts
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.SelectionMode
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.features.consent.ConsentTextBuilder
import com.stripe.android.financialconnections.features.consent.FinancialConnectionsUrlResolver
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
    private val logger: Logger,
    private val pollAuthorizationSessionAccounts: PollAuthorizationSessionAccounts
) : MavericksViewModel<AccountPickerState>(initialState) {

    init {
        suspend {
            val manifest = getManifest()
            val authSession = requireNotNull(manifest.activeAuthSession)
            val partnerAccountList = pollAuthorizationSessionAccounts(authSession.id)
            val accounts = partnerAccountList.data.map { account ->
                AccountPickerState.PartnerAccountUI(
                    account = account,
                    enabled = manifest.paymentMethodType == null ||
                        account.supportedPaymentMethodTypes.contains(manifest.paymentMethodType)
                )
            }.sortedBy { it.enabled }
            AccountPickerState.Payload(
                accounts = accounts,
                selectionMode = SelectionMode.CHECKBOXES, // TODO choose proper mode.
                accessibleData = AccessibleDataCalloutModel(
                    businessName = ConsentTextBuilder.getBusinessName(manifest),
                    permissions = manifest.permissions,
                    isStripeDirect = manifest.isStripeDirect ?: false,
                    dataPolicyUrl = FinancialConnectionsUrlResolver.getDataPolicyUrl(manifest)
                )
            )
        }.execute {
            copy(
                payload = it
            )
        }
    }

    fun onAccountClicked(account: PartnerAccount) {
        withState { state ->
            when (state.payload()?.selectionMode) {
                SelectionMode.DROPDOWN,
                SelectionMode.RADIO -> setState { copy(selectedIds = setOf(account.id)) }
                SelectionMode.CHECKBOXES -> if (state.selectedIds.contains(account.id)) {
                    setState { copy(selectedIds = selectedIds - account.id) }
                } else {
                    setState { copy(selectedIds = selectedIds + account.id) }
                }
                null -> logger.error("account clicked without available payload.")
            }
        }
    }

    fun selectAccounts() {
        withState { state ->
            state.payload()?.accounts?.let { accounts ->
                suspend {
                    val manifest = getManifest()
                    val accountsList: PartnerAccountsList = selectAccounts(
                        selectedAccounts = accounts
                            .filter { state.selectedIds.contains(it.account.id) }
                            .map { it.account },
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
    val payload: Async<Payload> = Uninitialized,
    val selectAccounts: Async<PartnerAccountsList> = Uninitialized,
    val selectedIds: Set<String> = emptySet()
) : MavericksState {

    val isLoading: Boolean
        get() = payload is Loading || selectAccounts is Loading

    data class Payload(
        val accounts: List<PartnerAccountUI>,
        val selectionMode: SelectionMode,
        val accessibleData: AccessibleDataCalloutModel
    )

    data class PartnerAccountUI(
        val account: PartnerAccount,
        val enabled: Boolean
    )

    enum class SelectionMode {
        DROPDOWN, RADIO, CHECKBOXES
    }
}
