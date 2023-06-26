package com.stripe.android.financialconnections.features.linkaccountpicker

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Click
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ClickLearnMoreDataAccess
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Error
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PaneLoaded
import com.stripe.android.financialconnections.domain.FetchNetworkedAccounts
import com.stripe.android.financialconnections.domain.GetCachedConsumerSession
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.SelectNetworkedAccount
import com.stripe.android.financialconnections.domain.UpdateCachedAccounts
import com.stripe.android.financialconnections.domain.UpdateLocalManifest
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.features.consent.FinancialConnectionsUrlResolver
import com.stripe.android.financialconnections.model.AddNewAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount.Status
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.NetworkedAccount
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class LinkAccountPickerViewModel @Inject constructor(
    initialState: LinkAccountPickerState,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getCachedConsumerSession: GetCachedConsumerSession,
    private val fetchNetworkedAccounts: FetchNetworkedAccounts,
    private val selectNetworkedAccount: SelectNetworkedAccount,
    private val updateLocalManifest: UpdateLocalManifest,
    private val updateCachedAccounts: UpdateCachedAccounts,
    private val getManifest: GetManifest,
    private val goNext: GoNext,
    private val logger: Logger
) : MavericksViewModel<LinkAccountPickerState>(initialState) {

    init {
        observeAsyncs()
        suspend {
            val manifest = getManifest()
            val accessibleData = AccessibleDataCalloutModel(
                businessName = manifest.businessName,
                permissions = manifest.permissions,
                isNetworking = true,
                isStripeDirect = manifest.isStripeDirect ?: false,
                dataPolicyUrl = FinancialConnectionsUrlResolver.getDataPolicyUrl(manifest)
            )
            val consumerSession = requireNotNull(getCachedConsumerSession())
            val accountsResponse = fetchNetworkedAccounts(consumerSession.clientSecret)
            val display = requireNotNull(
                accountsResponse
                    .display?.text?.returningNetworkingUserAccountPicker
            )
            // zip the accounts with their display info by id.
            val accounts = requireNotNull(accountsResponse.data)
                .map { partnerAccount ->
                    Pair(
                        partnerAccount,
                        display.accounts.first { partnerAccount.id == it.id }
                    )
                }
            eventTracker.track(PaneLoaded(PANE))
            LinkAccountPickerState.Payload(
                accounts = accounts,
                addNewAccount = requireNotNull(display.addNewAccount),
                title = display.title,
                defaultCta = display.defaultCta,
                repairAuthorizationEnabled = accountsResponse.repairAuthorizationEnabled,
                stepUpAuthenticationRequired = manifest.stepUpAuthenticationRequired ?: false,
                consumerSessionClientSecret = consumerSession.clientSecret,
                businessName = manifest.businessName,
                // We always want to refer to Link rather than Stripe on Link panes.
                accessibleData = accessibleData.copy(isStripeDirect = false)
            )
        }.execute { copy(payload = it) }
    }

    private fun observeAsyncs() {
        onAsync(
            LinkAccountPickerState::payload,
            onFail = { error ->
                logger.error("Error fetching payload", error)
                eventTracker.track(Error(PANE, error))
                goNext(Pane.INSTITUTION_PICKER)
            },
        )
        onAsync(
            LinkAccountPickerState::selectNetworkedAccountAsync,
            onFail = { error ->
                logger.error("Error selecting networked account", error)
                eventTracker.track(Error(PANE, error))
            },
        )
    }

    fun onLearnMoreAboutDataAccessClick() {
        // navigation to learn more about data access happens within the view component.
        viewModelScope.launch { eventTracker.track(ClickLearnMoreDataAccess(PANE)) }
    }

    fun onNewBankAccountClick() = viewModelScope.launch {
        eventTracker.track(Click("click.new_account", PANE))
        val nextPane = awaitState().payload()?.addNewAccount?.nextPane ?: Pane.INSTITUTION_PICKER
        goNext(nextPane)
    }

    fun onSelectAccountClick() = suspend {
        val state = awaitState()
        val payload = requireNotNull(state.payload())
        val selectedAccount =
            requireNotNull(payload.accounts.first { it.first.id == state.selectedAccountId })
        when {
            selectedAccount.first.status != Status.ACTIVE -> repairAccount()
            payload.stepUpAuthenticationRequired -> goNext(Pane.LINK_STEP_UP_VERIFICATION)
            else -> selectAccount(payload, selectedAccount.first)
        }
        Unit
    }.execute { copy(selectNetworkedAccountAsync = it) }

    private suspend fun repairAccount() {
        eventTracker.track(Click("click.repair_accounts", PANE))
        TODO("Account repair flow not yet implemented")
    }

    private suspend fun selectAccount(
        payload: LinkAccountPickerState.Payload,
        selectedAccount: PartnerAccount
    ) {
        val activeInstitution = selectNetworkedAccount(
            consumerSessionClientSecret = payload.consumerSessionClientSecret,
            selectedAccountId = selectedAccount.id
        )
        // Updates manifest active institution after account networked.
        updateLocalManifest { it.copy(activeInstitution = activeInstitution.data.firstOrNull()) }
        // Updates cached accounts with the one selected.
        updateCachedAccounts { listOf(selectedAccount) }
        eventTracker.track(Click("click.link_accounts", PANE))
        goNext(Pane.SUCCESS)
    }

    fun onAccountClick(partnerAccount: PartnerAccount) {
        setState { copy(selectedAccountId = partnerAccount.id) }
    }

    companion object :
        MavericksViewModelFactory<LinkAccountPickerViewModel, LinkAccountPickerState> {

        internal val PANE = Pane.LINK_ACCOUNT_PICKER

        override fun create(
            viewModelContext: ViewModelContext,
            state: LinkAccountPickerState
        ): LinkAccountPickerViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .linkAccountPickerSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class LinkAccountPickerState(
    val payload: Async<Payload> = Uninitialized,
    val selectNetworkedAccountAsync: Async<Unit> = Uninitialized,
    val selectedAccountId: String? = null,
) : MavericksState {

    data class Payload(
        val title: String,
        val accounts: List<Pair<PartnerAccount, NetworkedAccount>>,
        val addNewAccount: AddNewAccount,
        val accessibleData: AccessibleDataCalloutModel,
        val businessName: String?,
        val consumerSessionClientSecret: String,
        val repairAuthorizationEnabled: Boolean,
        val stepUpAuthenticationRequired: Boolean,
        val defaultCta: String
    )

    val cta: String?
        get() = payload()?.let { payload ->
            payload.accounts.firstOrNull { it.first.id == selectedAccountId }
                ?.second?.selectionCta
                ?: payload.defaultCta
        }
}
