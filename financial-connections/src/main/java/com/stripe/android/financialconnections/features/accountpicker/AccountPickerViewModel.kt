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
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionAccounts
import com.stripe.android.financialconnections.domain.SelectAccounts
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.SelectionMode
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.features.consent.ConsentTextBuilder
import com.stripe.android.financialconnections.features.consent.FinancialConnectionsUrlResolver
import com.stripe.android.financialconnections.features.partnerauth.isOAuth
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

@Suppress("LongParameterList")
internal class AccountPickerViewModel @Inject constructor(
    initialState: AccountPickerState,
    private val selectAccounts: SelectAccounts,
    private val getManifest: GetManifest,
    private val goNext: GoNext,
    private val navigationManager: NavigationManager,
    private val logger: Logger,
    private val pollAuthorizationSessionAccounts: PollAuthorizationSessionAccounts
) : MavericksViewModel<AccountPickerState>(initialState) {

    init {
        logErrors()
        suspend {
            val manifest = getManifest()
            val partnerAccountList = pollAuthorizationSessionAccounts(manifest)
            val accounts = partnerAccountList.data.map { account ->
                AccountPickerState.PartnerAccountUI(
                    account = account,
                    enabled = account.enabled(manifest)
                )
            }.sortedBy { it.enabled }
            val (preselectedIds, selectionMode) = selectionConfig(accounts, manifest)
            AccountPickerState.Payload(
                accounts = accounts,
                selectionMode = selectionMode,
                accessibleData = AccessibleDataCalloutModel(
                    businessName = ConsentTextBuilder.getBusinessName(manifest),
                    permissions = manifest.permissions,
                    isStripeDirect = manifest.isStripeDirect ?: false,
                    dataPolicyUrl = FinancialConnectionsUrlResolver.getDataPolicyUrl(manifest)
                ),
                selectedIds = preselectedIds
            )
        }.execute { copy(payload = it) }
    }

    private fun logErrors() {
        onAsync(AccountPickerState::payload, onFail = {
            logger.error("Error retrieving accounts", it)
        })
        onAsync(AccountPickerState::selectAccounts, onFail = {
            logger.error("Error selecting accounts", it)
        })
    }

    /**
     * in the special case that this is single account and the institution would have
     * skipped account selection but _didn't_ (because we still saw this), we should
     * render the variant of the AccountPicker which uses a select dropdown. This is
     * meant to prevent showing a radio-button account picker immediately after the user
     * interacted with a checkbox select picker, which is the predominant UX of oauth popovers today.
     */
    private fun selectionConfig(
        accounts: List<AccountPickerState.PartnerAccountUI>,
        manifest: FinancialConnectionsSessionManifest
    ): Pair<Set<String>, SelectionMode> =
        when {
            manifest.singleAccount -> when {
                manifest.activeAuthSession?.skipAccountSelection == true &&
                    manifest.activeAuthSession.flow?.isOAuth() == true -> Pair(
                    setOfNotNull(accounts.firstOrNull { it.enabled }?.account?.id),
                    SelectionMode.DROPDOWN
                )
                else -> emptySet<String>() to SelectionMode.RADIO
            }
            else -> emptySet<String>() to SelectionMode.CHECKBOXES
        }

    private fun PartnerAccount.enabled(
        manifest: FinancialConnectionsSessionManifest
    ) = manifest.paymentMethodType == null ||
        supportedPaymentMethodTypes.contains(manifest.paymentMethodType)

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
                    goNext(accountsList.nextPane)
                    accountsList
                }.execute {
                    copy(selectAccounts = it)
                }
            }
        }
    }

    fun selectAnotherBank() {
        navigationManager.navigate(NavigationDirections.institutionPicker)
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
        val accessibleData: AccessibleDataCalloutModel,
        val selectedIds: Set<String>
    )

    data class PartnerAccountUI(
        val account: PartnerAccount,
        val enabled: Boolean
    )

    enum class SelectionMode {
        DROPDOWN, RADIO, CHECKBOXES
    }
}
