package com.stripe.android.financialconnections.features.linkaccountpicker

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.AccountsSubmitted
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Click
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ClickLearnMoreDataAccess
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.FetchNetworkedAccounts
import com.stripe.android.financialconnections.domain.GetCachedConsumerSession
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.SelectNetworkedAccounts
import com.stripe.android.financialconnections.domain.UpdateCachedAccounts
import com.stripe.android.financialconnections.exception.UnclassifiedError
import com.stripe.android.financialconnections.features.accountupdate.AccountUpdateRequiredState
import com.stripe.android.financialconnections.features.accountupdate.AccountUpdateRequiredState.Type.PartnerAuth
import com.stripe.android.financialconnections.features.accountupdate.AccountUpdateRequiredState.Type.Repair
import com.stripe.android.financialconnections.features.accountupdate.PresentAccountUpdateRequiredSheet
import com.stripe.android.financialconnections.features.common.MerchantDataAccessModel
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerClickableText.DATA
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.DataAccess
import com.stripe.android.financialconnections.features.notice.PresentNoticeSheet
import com.stripe.android.financialconnections.model.AddNewAccount
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.BANK_AUTH_REPAIR
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.PARTNER_AUTH
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.SUCCESS
import com.stripe.android.financialconnections.model.NetworkedAccount
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.Destination.InstitutionPicker
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.ui.HandleClickableUrl
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.utils.error
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import java.util.Date

internal class LinkAccountPickerViewModel @AssistedInject constructor(
    @Assisted initialState: LinkAccountPickerState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getCachedConsumerSession: GetCachedConsumerSession,
    private val handleClickableUrl: HandleClickableUrl,
    private val fetchNetworkedAccounts: FetchNetworkedAccounts,
    private val selectNetworkedAccounts: SelectNetworkedAccounts,
    private val updateCachedAccounts: UpdateCachedAccounts,
    private val getSync: GetOrFetchSync,
    private val navigationManager: NavigationManager,
    private val logger: Logger,
    private val presentNoticeSheet: PresentNoticeSheet,
    private val presentUpdateRequiredSheet: PresentAccountUpdateRequiredSheet,
) : FinancialConnectionsViewModel<LinkAccountPickerState>(initialState, nativeAuthFlowCoordinator) {

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
                merchantDataAccess = merchantDataAccess.copy(isStripeDirect = false),
                singleAccount = manifest.singleAccount,
            )
        }.execute { copy(payload = it) }
    }

    override fun updateTopAppBar(state: LinkAccountPickerState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = PANE,
            allowBackNavigation = false,
            error = state.payload.error,
        )
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
                navigationManager.tryNavigateTo(InstitutionPicker(referrer = PANE))
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
                    presentDataAccessBottomSheet()
                }
            )
        )
    }

    private fun presentDataAccessBottomSheet() {
        val dataAccessNotice = stateFlow.value.payload()?.dataAccessNotice ?: return
        eventTracker.track(ClickLearnMoreDataAccess(PANE))
        presentNoticeSheet(
            content = DataAccess(dataAccessNotice),
            referrer = PANE,
        )
    }

    fun onNewBankAccountClick() = viewModelScope.launch {
        eventTracker.track(Click("click.new_account", PANE))
        val nextPane = stateFlow.value.payload()?.nextPaneOnNewAccount ?: Pane.INSTITUTION_PICKER
        navigationManager.tryNavigateTo(nextPane.destination(referrer = PANE))
    }

    fun onSelectAccountsClick() {
        suspend {
            val state = stateFlow.value
            val payload = requireNotNull(state.payload())

            val accounts = payload.selectedPartnerAccounts(state.selectedAccountIds)
            updateCachedAccounts(accounts)

            // We assume that at this point, all selected accounts have the same next pane.
            // Otherwise, the user would have been presented with an update-required bottom
            // sheet before.
            val nextPane = accounts.lastOrNull()?.nextPaneOnSelection
            val accountIds = accounts.map { it.id }.toSet()

            eventTracker.track(
                AccountsSubmitted(
                    accountIds = accountIds,
                    isSkipAccountSelection = false,
                    pane = PANE
                )
            )

            eventTracker.track(Click("click.link_accounts", PANE))

            if (nextPane == SUCCESS) {
                selectAccounts(
                    consumerSessionClientSecret = payload.consumerSessionClientSecret,
                    accountIds = accountIds,
                )
            } else {
                handleUnsupportedNextPane(nextPane)
            }
        }.execute { copy(selectNetworkedAccountAsync = it) }
    }

    private suspend fun selectAccounts(
        consumerSessionClientSecret: String,
        accountIds: Set<String>,
    ) {
        selectNetworkedAccounts(
            consumerSessionClientSecret = consumerSessionClientSecret,
            selectedAccountIds = accountIds,
        )
        FinancialConnections.emitEvent(name = Name.ACCOUNTS_SELECTED)
        navigationManager.tryNavigateTo(Destination.Success(referrer = PANE))
    }

    private fun handleUnsupportedNextPane(nextPane: Pane?) {
        when (nextPane) {
            PARTNER_AUTH -> {
                eventTracker.logError(
                    extraMessage = "Connecting a supportability account, but user shouldn't be able to.",
                    error = UnclassifiedError("ConnectSupportabilityAccountError"),
                    logger = logger,
                    pane = PANE,
                )
            }
            BANK_AUTH_REPAIR -> {
                eventTracker.logError(
                    extraMessage = "Connecting a repair account, but user shouldn't be able to.",
                    error = UnclassifiedError("ConnectRepairAccountError"),
                    logger = logger,
                    pane = PANE,
                )
            }
            null -> {
                eventTracker.logError(
                    extraMessage = "Selected connect account, but next pane is NULL.",
                    error = UnclassifiedError("ConnectUnselectedAccountError"),
                    logger = logger,
                    pane = PANE,
                )
            }
            else -> {
                // Nothing to log here
            }
        }

        // Fall back to the institution picker
        val destination = nextPane?.destination?.invoke(referrer = PANE) ?: InstitutionPicker(referrer = PANE)
        navigationManager.tryNavigateTo(destination)
    }

    private fun logAccountClick(partnerAccount: PartnerAccount) {
        val state = stateFlow.value
        val payload = state.payload() ?: return
        val isNewSelection = partnerAccount.id !in state.selectedAccountIds

        val event = FinancialConnectionsAnalyticsEvent.AccountSelected(
            pane = PANE,
            selected = isNewSelection,
            isSingleAccount = payload.singleAccount,
            accountId = partnerAccount.id,
        )

        eventTracker.track(event)
    }

    fun onAccountClick(partnerAccount: PartnerAccount) = viewModelScope.launch {
        logAccountClick(partnerAccount)

        val accounts = requireNotNull(stateFlow.value.payload()?.accounts)
        val drawerOnSelection = accounts.firstOrNull { it.first.id == partnerAccount.id }?.second?.drawerOnSelection

        if (drawerOnSelection != null) {
            // TODO@carlosmuvi handle drawer display.
            logger.debug("Drawer on selection: $drawerOnSelection")
            return@launch
        }

        val updateRequired = createUpdateRequiredContent(
            partnerAccount = partnerAccount,
            partnerToCoreAuths = stateFlow.value.payload()?.partnerToCoreAuths,
        )

        if (updateRequired != null) {
            logUpdateRequired(updateRequired)
            presentUpdateRequiredSheet(updateRequired, referrer = PANE)
            return@launch
        }

        setState {
            val payload = requireNotNull(payload())
            copy(
                selectedAccountIds = when {
                    payload.singleAccount -> listOf(partnerAccount.id)
                    partnerAccount.id in selectedAccountIds -> selectedAccountIds - partnerAccount.id
                    else -> selectedAccountIds + partnerAccount.id
                }
            )
        }
    }

    private fun logUpdateRequired(state: AccountUpdateRequiredState.Payload) {
        val eventName = when (state.type) {
            is PartnerAuth -> "click.supportability_account"
            is Repair -> "click.repair_accounts"
        }

        eventTracker.track(Click(eventName, pane = PANE))
    }

    fun onViewEffectLaunched() {
        setState { copy(viewEffect = null) }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: LinkAccountPickerState): LinkAccountPickerViewModel
    }

    companion object {

        internal val PANE = Pane.LINK_ACCOUNT_PICKER

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent.linkAccountPickerViewModelFactory.create(LinkAccountPickerState())
                }
            }
    }
}

internal data class LinkAccountPickerState(
    val payload: Async<Payload> = Uninitialized,
    val selectNetworkedAccountAsync: Async<Unit> = Uninitialized,
    val selectedAccountIds: List<String> = emptyList(),
    val viewEffect: ViewEffect? = null,
) {

    data class Payload(
        val title: String,
        val accounts: List<Pair<PartnerAccount, NetworkedAccount>>,
        val dataAccessNotice: DataAccessNotice?,
        val addNewAccount: AddNewAccount,
        val merchantDataAccess: MerchantDataAccessModel,
        val consumerSessionClientSecret: String,
        val defaultCta: String,
        val nextPaneOnNewAccount: Pane?,
        val partnerToCoreAuths: Map<String, String>?,
        val singleAccount: Boolean,
    ) {

        fun selectedPartnerAccounts(selected: List<String>): List<PartnerAccount> {
            return accounts.filter { it.first.id in selected }.map { it.first }
        }
    }

    val cta: TextResource
        get() {
            val payload = payload()

            return if (payload?.singleAccount == true) {
                val selectedAccount = payload.accounts.singleOrNull { (partnerAccount, _) ->
                    partnerAccount.id in selectedAccountIds
                }?.second

                TextResource.Text(
                    value = selectedAccount?.selectionCta ?: payload.defaultCta,
                )
            } else {
                TextResource.Text(
                    value = payload?.defaultCta.orEmpty(),
                )
            }
        }

    sealed class ViewEffect {
        data class OpenUrl(
            val url: String,
            val id: Long
        ) : ViewEffect()
    }
}

internal enum class LinkAccountPickerClickableText(val value: String) {
    DATA("stripe://data-access-notice"),
}

private fun createUpdateRequiredContent(
    partnerAccount: PartnerAccount,
    partnerToCoreAuths: Map<String, String>?,
): AccountUpdateRequiredState.Payload? {
    return when (partnerAccount.nextPaneOnSelection) {
        BANK_AUTH_REPAIR -> {
            AccountUpdateRequiredState.Payload(
                iconUrl = partnerAccount.institution?.icon?.default,
                type = Repair(
                    authorization = partnerAccount.authorization?.let { partnerToCoreAuths?.getValue(it) },
                ),
            )
        }
        PARTNER_AUTH -> {
            AccountUpdateRequiredState.Payload(
                iconUrl = partnerAccount.institution?.icon?.default,
                type = PartnerAuth(
                    institution = partnerAccount.institution,
                ),
            )
        }
        else -> {
            null
        }
    }
}
