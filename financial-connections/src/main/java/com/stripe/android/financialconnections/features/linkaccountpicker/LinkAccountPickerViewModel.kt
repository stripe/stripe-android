package com.stripe.android.financialconnections.features.linkaccountpicker

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Click
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ClickLearnMoreDataAccess
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.domain.FetchNetworkedAccounts
import com.stripe.android.financialconnections.domain.GetCachedConsumerSession
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.SelectNetworkedAccount
import com.stripe.android.financialconnections.domain.UpdateCachedAccounts
import com.stripe.android.financialconnections.domain.UpdateLocalManifest
import com.stripe.android.financialconnections.features.common.MerchantDataAccessModel
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerClickableText.DATA
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerState.ViewEffect.OpenBottomSheet
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.AddNewAccount
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.NetworkedAccount
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.repository.CoreAuthorizationPendingNetworkingRepairRepository
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.ui.HandleClickableUrl
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

internal class LinkAccountPickerViewModel @Inject constructor(
    initialState: LinkAccountPickerState,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getCachedConsumerSession: GetCachedConsumerSession,
    private val handleClickableUrl: HandleClickableUrl,
    private val fetchNetworkedAccounts: FetchNetworkedAccounts,
    private val selectNetworkedAccount: SelectNetworkedAccount,
    private val updateLocalManifest: UpdateLocalManifest,
    private val updateCachedAccounts: UpdateCachedAccounts,
    private val coreAuthorizationPendingNetworkingRepair: CoreAuthorizationPendingNetworkingRepairRepository,
    private val getSync: GetOrFetchSync,
    private val navigationManager: NavigationManager,
    private val logger: Logger
) : MavericksViewModel<LinkAccountPickerState>(initialState) {

    init {
        observeAsyncs()
        suspend {
            val sync = getSync()
            val manifest = sync.manifest
            val dataAccessNotice = sync.text?.consent?.dataAccessNotice
            val merchantDataAccess = MerchantDataAccessModel(
                businessName = manifest.businessName,
                permissions = manifest.permissions,
                isStripeDirect = manifest.isStripeDirect ?: false
            )
            val consumerSession = requireNotNull(getCachedConsumerSession())
            val accountsResponse = fetchNetworkedAccounts(consumerSession.clientSecret)
            val display = requireNotNull(
                accountsResponse
                    .display?.text?.returningNetworkingUserAccountPicker
            )
            // zip the accounts with their display info by id.
            val accounts = display.accounts.mapNotNull { networkedAccount: NetworkedAccount ->
                accountsResponse.data.firstOrNull { it.id == networkedAccount.id }
                    ?.let { matchingPartnerAccount ->
                        Pair(matchingPartnerAccount, networkedAccount)
                    }
            }

            eventTracker.track(PaneLoaded(PANE))
            LinkAccountPickerState.Payload(
                dataAccessNotice = dataAccessNotice,
                partnerToCoreAuths = accountsResponse.partnerToCoreAuths,
                accounts = accounts,
                nextPaneOnNewAccount = accountsResponse.nextPaneOnAddAccount,
                addNewAccount = requireNotNull(display.addNewAccount),
                title = display.title,
                defaultCta = display.defaultCta,
                consumerSessionClientSecret = consumerSession.clientSecret,
                // We always want to refer to Link rather than Stripe on Link panes.
                merchantDataAccess = merchantDataAccess.copy(isStripeDirect = false)
            )
        }.execute { copy(payload = it) }
    }

    private fun observeAsyncs() {
        onAsync(
            LinkAccountPickerState::payload,
            onFail = { error ->
                eventTracker.logError(
                    extraMessage = "Error fetching payload",
                    error = error,
                    logger = logger,
                    pane = PANE
                )
                navigationManager.tryNavigateTo(Destination.InstitutionPicker(referrer = PANE))
            },
        )
        onAsync(
            LinkAccountPickerState::selectNetworkedAccountAsync,
            onFail = { error ->
                eventTracker.logError(
                    extraMessage = "Error selecting networked account",
                    error = error,
                    logger = logger,
                    pane = PANE
                )
            },
        )
    }

    fun onClickableTextClick(uri: String) = viewModelScope.launch {
        val date = Date()
        handleClickableUrl(
            currentPane = PANE,
            uri = uri,
            onNetworkUrlClicked = {
                setState { copy(viewEffect = OpenUrl(uri, date.time)) }
            },
            knownDeeplinkActions = mapOf(
                DATA.value to {
                    eventTracker.track(ClickLearnMoreDataAccess(PANE))
                    setState { copy(viewEffect = OpenBottomSheet(date.time)) }
                }
            )
        )
    }

    fun onNewBankAccountClick() = viewModelScope.launch {
        eventTracker.track(Click("click.new_account", PANE))
        val nextPane = awaitState().payload()?.nextPaneOnNewAccount ?: Pane.INSTITUTION_PICKER
        navigationManager.tryNavigateTo(nextPane.destination(referrer = PANE))
    }

    fun onSelectAccountClick() = suspend {
        val state = awaitState()
        val payload = requireNotNull(state.payload())
        val (account, _) =
            requireNotNull(payload.accounts.first { it.first.id == state.selectedAccountId })
        val nextPane = account.nextPaneOnSelection
        // Caches the selected account.
        updateCachedAccounts { listOf(account) }
        when (nextPane) {
            Pane.SUCCESS -> {
                val activeInstitution = selectNetworkedAccount(
                    consumerSessionClientSecret = payload.consumerSessionClientSecret,
                    selectedAccountId = account.id
                )
                // Updates manifest active institution after account networked.
                updateLocalManifest { it.copy(activeInstitution = activeInstitution.data.firstOrNull()) }
                // Updates cached accounts with the one selected.
                eventTracker.track(Click("click.link_accounts", PANE))
            }

            Pane.BANK_AUTH_REPAIR -> {
                coreAuthorizationPendingNetworkingRepair.set(
                    requireNotNull(payload.partnerToCoreAuths).getValue(account.authorization)
                )
                eventTracker.track(Click("click.repair_accounts", PANE))
            }

            Pane.PARTNER_AUTH -> {
                updateLocalManifest { it.copy(activeInstitution = account.institution) }
            }

            else -> Unit
        }
        nextPane?.let {
            FinancialConnections.emitEvent(name = Name.ACCOUNTS_SELECTED)
            navigationManager.tryNavigateTo(it.destination(referrer = PANE))
        }
        Unit
    }.execute { copy(selectNetworkedAccountAsync = it) }

    fun onAccountClick(partnerAccount: PartnerAccount) {
        setState { copy(selectedAccountId = partnerAccount.id) }
    }

    fun onViewEffectLaunched() {
        setState { copy(viewEffect = null) }
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
    val viewEffect: ViewEffect? = null
) : MavericksState {

    data class Payload(
        val title: String,
        val accounts: List<Pair<PartnerAccount, NetworkedAccount>>,
        val dataAccessNotice: DataAccessNotice?,
        val addNewAccount: AddNewAccount,
        val merchantDataAccess: MerchantDataAccessModel,
        val consumerSessionClientSecret: String,
        val defaultCta: String,
        val nextPaneOnNewAccount: Pane?,
        val partnerToCoreAuths: Map<String, String>?
    )

    val cta: String?
        get() = payload()?.let { payload ->
            payload.accounts.firstOrNull { it.first.id == selectedAccountId }
                ?.second?.selectionCta
                ?: payload.defaultCta
        }

    sealed class ViewEffect {
        data class OpenUrl(
            val url: String,
            val id: Long
        ) : ViewEffect()

        data class OpenBottomSheet(
            val id: Long
        ) : ViewEffect()
    }
}

internal enum class LinkAccountPickerClickableText(val value: String) {
    DATA("stripe://data-access-notice"),
}
