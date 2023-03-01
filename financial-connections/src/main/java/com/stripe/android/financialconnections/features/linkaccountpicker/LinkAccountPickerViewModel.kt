package com.stripe.android.financialconnections.features.linkaccountpicker

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Error
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PaneLoaded
import com.stripe.android.financialconnections.domain.GetCachedConsumerSession
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.PollNetworkedAccounts
import com.stripe.android.financialconnections.domain.SelectNetworkedAccount
import com.stripe.android.financialconnections.domain.UpdateCachedAccounts
import com.stripe.android.financialconnections.domain.UpdateLocalManifest
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.features.common.getBusinessName
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount.Status
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class LinkAccountPickerViewModel @Inject constructor(
    initialState: LinkAccountPickerState,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getCachedConsumerSession: GetCachedConsumerSession,
    private val pollNetworkedAccounts: PollNetworkedAccounts,
    private val selectNetworkedAccount: SelectNetworkedAccount,
    private val updateLocalManifest: UpdateLocalManifest,
    private val updateCachedAccounts: UpdateCachedAccounts,
    private val getManifest: GetManifest,
    private val goNext: GoNext,
    private val logger: Logger
) : MavericksViewModel<LinkAccountPickerState>(initialState) {

    init {
        logErrors()
        suspend {
            val manifest = getManifest()
            val accessibleData = AccessibleDataCalloutModel.fromManifest(manifest)
            val consumerSession = requireNotNull(getCachedConsumerSession())
            val accounts = pollNetworkedAccounts(consumerSession.clientSecret)
                .data
                .sortedBy { it.allowSelection.not() }
            eventTracker.track(PaneLoaded(Pane.LINK_ACCOUNT_PICKER))
            LinkAccountPickerState.Payload(
                stepUpAuthenticationRequired = manifest.stepUpAuthenticationRequired,
                consumerSessionClientSecret = consumerSession.clientSecret,
                businessName = manifest.getBusinessName() ?: "",
                accounts = accounts,
                accessibleData = accessibleData
            )
        }.execute { copy(payload = it) }
    }

    private fun logErrors() {
        onAsync(
            LinkAccountPickerState::payload,
            onFail = { error ->
                logger.error("Error fetching payload", error)
                eventTracker.track(Error(Pane.LINK_ACCOUNT_PICKER, error))
            },
        )
        onAsync(
            LinkAccountPickerState::selectNetworkedAccountAsync,
            onFail = { error ->
                logger.error("Error selecting networked account", error)
                eventTracker.track(Error(Pane.LINK_ACCOUNT_PICKER, error))
            },
        )
    }

    fun onLearnMoreAboutDataAccessClick() {
        viewModelScope.launch {
            eventTracker.track(
                FinancialConnectionsEvent.ClickLearnMoreDataAccess(Pane.LINK_STEP_UP_VERIFICATION)
            )
        }
    }

    fun onNewBankAccountClick() {
        goNext(Pane.INSTITUTION_PICKER)
    }

    fun onSelectAccountClick() = suspend {
        val state = awaitState()
        val payload = requireNotNull(state.payload())
        val selectedAccount =
            requireNotNull(payload.accounts.first { it.id == state.selectedAccountId })
        when {
            selectedAccount.status != Status.ACTIVE -> repairAccount()
            payload.stepUpAuthenticationRequired == true -> goNext(Pane.LINK_STEP_UP_VERIFICATION)
            else -> selectAccount(payload, selectedAccount)
        }
        Unit
    }.execute { copy(selectNetworkedAccountAsync = it) }

    private fun repairAccount() {
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
        goNext(Pane.SUCCESS)
    }

    fun onAccountClick(partnerAccount: PartnerAccount) {
        setState { copy(selectedAccountId = partnerAccount.id) }
    }

    companion object :
        MavericksViewModelFactory<LinkAccountPickerViewModel, LinkAccountPickerState> {

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
        val accounts: List<PartnerAccount>,
        val accessibleData: AccessibleDataCalloutModel,
        val businessName: String,
        val consumerSessionClientSecret: String,
        val stepUpAuthenticationRequired: Boolean?
    )
}
