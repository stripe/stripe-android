package com.stripe.android.financialconnections.features.linkaccountpicker

import FinancialConnectionsGenericInfoScreen
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
import com.stripe.android.financialconnections.domain.AcceptConsent
import com.stripe.android.financialconnections.domain.FetchNetworkedAccounts
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.SelectNetworkedAccounts
import com.stripe.android.financialconnections.domain.UpdateCachedAccounts
import com.stripe.android.financialconnections.exception.UnclassifiedError
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerClickableText.DATA
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.features.notice.NoticeSheetState
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.DataAccess
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.Generic
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.UpdateRequired
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.UpdateRequired.Type
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.UpdateRequired.Type.Repair
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.UpdateRequired.Type.Supportability
import com.stripe.android.financialconnections.features.notice.PresentSheet
import com.stripe.android.financialconnections.model.AddNewAccount
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.BANK_AUTH_REPAIR
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.INSTITUTION_PICKER
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.PARTNER_AUTH
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.SUCCESS
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.NetworkedAccount
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.navigation.Destination.InstitutionPicker
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.PopUpToBehavior
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.repository.ConsumerSessionProvider
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
    private val consumerSessionProvider: ConsumerSessionProvider,
    private val handleClickableUrl: HandleClickableUrl,
    private val fetchNetworkedAccounts: FetchNetworkedAccounts,
    private val selectNetworkedAccounts: SelectNetworkedAccounts,
    private val updateCachedAccounts: UpdateCachedAccounts,
    private val getSync: GetOrFetchSync,
    private val navigationManager: NavigationManager,
    private val logger: Logger,
    private val acceptConsent: AcceptConsent,
    private val presentSheet: PresentSheet,
) : FinancialConnectionsViewModel<LinkAccountPickerState>(initialState, nativeAuthFlowCoordinator) {

    init {
        observeAsyncs()
        suspend {
            val sync = getSync()
            val manifest = sync.manifest
            val consumerSession = requireNotNull(consumerSessionProvider.provideConsumerSession())
            val accountsResponse = fetchNetworkedAccounts(consumerSession.clientSecret)
            val display = requireNotNull(
                accountsResponse
                    .display?.text?.returningNetworkingUserAccountPicker
            )
            // zip the accounts with their display info by id.
            val accounts = display.accounts.mapNotNull { networkedAccount: NetworkedAccount ->
                accountsResponse.data.firstOrNull { it.id == networkedAccount.id }
                    ?.let { matchingPartnerAccount ->
                        LinkedAccount(matchingPartnerAccount, networkedAccount)
                    }
            }

            // When accounts load, preselect the first selectable account
            val selectedAccountIds = listOfNotNull(
                accounts
                    .firstOrNull { account ->
                        account.display.allowSelection &&
                            account.display.drawerOnSelection == null
                    }
                    ?.account?.id
            )

            eventTracker.track(PaneLoaded(PANE))
            LinkAccountPickerState.Payload(
                partnerToCoreAuths = accountsResponse.partnerToCoreAuths,
                accounts = accounts,
                aboveCta = display.aboveCta,
                defaultDataAccessNotice = sync.text?.consent?.dataAccessNotice,
                nextPaneOnNewAccount = accountsResponse.nextPaneOnAddAccount,
                multipleAccountTypesSelectedDataAccessNotice = display.multipleAccountTypesSelectedDataAccessNotice,
                addNewAccount = requireNotNull(display.addNewAccount),
                title = display.title,
                defaultCta = display.defaultCta,
                consumerSessionClientSecret = consumerSession.clientSecret,
                singleAccount = manifest.singleAccount,
                acquireConsentOnPrimaryCtaClick = accountsResponse.acquireConsentOnPrimaryCtaClick ?: false,
                selectedAccountIds = selectedAccountIds
            )
        }.execute {
            copy(payload = it)
        }
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
            onSuccess = { payload ->
                if (payload.accounts.isEmpty()) {
                    skipToNextPane(payload)
                }
            },
            onFail = { error ->
                eventTracker.logError(
                    extraMessage = "Error fetching payload",
                    error = error,
                    logger = logger,
                    pane = PANE
                )
                navigationManager.tryNavigateTo(InstitutionPicker(referrer = PANE))
            }
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

    private fun skipToNextPane(payload: LinkAccountPickerState.Payload) {
        val nextPane = payload.nextPaneOnNewAccount ?: Pane.INSTITUTION_PICKER

        navigationManager.tryNavigateTo(
            route = nextPane.destination(referrer = PANE),
            popUpTo = PopUpToBehavior.Route(
                // Prevent back navigation, since we're only showing an empty list
                route = Pane.CONSENT.destination.fullRoute,
                inclusive = true,
            ),
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
        val dataAccessNotice = stateFlow.value.activeDataAccessNotice ?: return
        eventTracker.track(ClickLearnMoreDataAccess(PANE))
        presentSheet(
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
            val accounts = payload.selectedAccounts.map { it.account }
            val selectedAccountDrawers = payload.selectedAccounts
                .mapNotNull { it.account.computeDrawerPayload(payload) }
            updateCachedAccounts(accounts)

            // In some cases we need to mark consent acquisition on submission and then need
            // to present the necessary drawer.
            if (selectedAccountDrawers.isNotEmpty()) {
                // Currently this should only occur for single account flows to not present a drawer right away but
                // to collect consent and then open the drawer. This is not expected to happen for multi-account flows.
                if (selectedAccountDrawers.size > 1) {
                    eventTracker.logError(
                        extraMessage = "Multiple accounts with drawers on selection",
                        error = UnclassifiedError("MultipleAccountsSelectedError"),
                        logger = logger,
                        pane = PANE
                    )
                }
                acceptConsent()
                selectedAccountDrawers.first().present()
            } else {
                // No account selected with drawers on selection.
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
                        acquireConsentOnPrimaryCtaClick = payload.acquireConsentOnPrimaryCtaClick,
                        consumerSessionClientSecret = payload.consumerSessionClientSecret,
                        accountIds = accountIds,
                    )
                } else {
                    handleNonSuccessNextPane(payload, nextPane)
                }
            }
            Unit
        }.execute { copy(selectNetworkedAccountAsync = it) }
    }

    private suspend fun selectAccounts(
        acquireConsentOnPrimaryCtaClick: Boolean,
        consumerSessionClientSecret: String,
        accountIds: Set<String>,
    ) {
        val response = selectNetworkedAccounts(
            consumerSessionClientSecret = consumerSessionClientSecret,
            selectedAccountIds = accountIds,
            consentAcquired = acquireConsentOnPrimaryCtaClick,
        )
        val nextPane = response.nextPane ?: SUCCESS

        FinancialConnections.emitEvent(name = Name.ACCOUNTS_SELECTED)
        navigationManager.tryNavigateTo(nextPane.destination(referrer = PANE))
    }

    private suspend fun handleNonSuccessNextPane(payload: LinkAccountPickerState.Payload, nextPane: Pane?) {
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

        if (payload.acquireConsentOnPrimaryCtaClick) {
            acceptConsent()
        }

        val overrideNextPane = when (nextPane) {
            PARTNER_AUTH,
            BANK_AUTH_REPAIR -> {
                // We don't support these panes here.
                INSTITUTION_PICKER
            }
            null -> INSTITUTION_PICKER
            else -> nextPane
        }

        val destination = overrideNextPane.destination(referrer = PANE)
        navigationManager.tryNavigateTo(destination)
    }

    private fun logAccountClick(partnerAccount: PartnerAccount) {
        val state = stateFlow.value
        val payload = state.payload() ?: return
        val isNewSelection = partnerAccount.id !in payload.selectedAccountIds

        val event = FinancialConnectionsAnalyticsEvent.AccountSelected(
            pane = PANE,
            selected = isNewSelection,
            isSingleAccount = payload.singleAccount,
            accountId = partnerAccount.id,
        )

        eventTracker.track(event)
    }

    fun onAccountClick(partnerAccount: PartnerAccount) {
        logAccountClick(partnerAccount)

        val payload = requireNotNull(stateFlow.value.payload())

        // Don't present the drawer if we need to acquire consent still, since the user has to explicitly consent
        // clicking the pane CTA.
        if (payload.acquireConsentOnPrimaryCtaClick.not()) {
            val drawerPayload = partnerAccount.computeDrawerPayload(payload)
            if (drawerPayload != null) {
                drawerPayload.present()
                return
            }
        }

        val selectedAccountIds = when {
            payload.singleAccount -> listOf(partnerAccount.id)
            partnerAccount.id in payload.selectedAccountIds -> payload.selectedAccountIds - partnerAccount.id
            else -> payload.selectedAccountIds + partnerAccount.id
        }

        setState {
            copy(
                payload = Success(payload.copy(selectedAccountIds = selectedAccountIds))
            )
        }
    }

    private fun PartnerAccount.computeDrawerPayload(
        payload: LinkAccountPickerState.Payload
    ): NoticeSheetState.NoticeSheetContent? {
        val drawerOnSelection = payload.accounts.firstOrNull { it.account.id == id }
            ?.display?.drawerOnSelection
        val updateRequired = drawerOnSelection
            // Use selected account icon (not coming on the SDU response as selection is not known by backend)
            ?.withIcon(institution?.icon?.default)
            ?.let { genericContent ->
                when (nextPaneOnSelection) {
                    BANK_AUTH_REPAIR -> UpdateRequired(
                        generic = genericContent,
                        type = Repair(
                            authorization = authorization?.let { payload.partnerToCoreAuths?.getValue(it) },
                        ),
                    )
                    PARTNER_AUTH -> UpdateRequired(
                        generic = genericContent,
                        type = Supportability(
                            institution = institution,
                        ),
                    )
                    INSTITUTION_PICKER -> UpdateRequired(
                        generic = genericContent,
                        type = Supportability(
                            institution = null,
                        ),
                    )
                    else -> null
                }
            }

        // [updateRequired] has to be checked before [drawerOnSelection].
        // The update required modal basically uses [drawerOnSelection] to render content,
        // and has specific logic handling for CTAs).
        return updateRequired ?: drawerOnSelection?.let(::Generic)
    }

    private fun NoticeSheetState.NoticeSheetContent.present() {
        if (this is UpdateRequired) logUpdateRequired(type)
        presentSheet(this, referrer = PANE)
    }

    private fun logUpdateRequired(type: Type) {
        val eventName = when (type) {
            is Supportability -> "click.supportability_account"
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
    val viewEffect: ViewEffect? = null,
) {

    data class Payload(
        val title: String,
        val accounts: List<LinkedAccount>,
        val selectedAccountIds: List<String>,
        val addNewAccount: AddNewAccount,
        val consumerSessionClientSecret: String,
        val defaultCta: String,
        val nextPaneOnNewAccount: Pane?,
        val partnerToCoreAuths: Map<String, String>?,
        val singleAccount: Boolean,
        val multipleAccountTypesSelectedDataAccessNotice: DataAccessNotice?,
        val aboveCta: String?,
        val defaultDataAccessNotice: DataAccessNotice?,
        val acquireConsentOnPrimaryCtaClick: Boolean,
    ) {

        val selectedAccounts: List<LinkedAccount>
            get() = accounts.filter { it.account.id in selectedAccountIds }
    }

    val activeDataAccessNotice: DataAccessNotice?
        get() {
            val payload = payload() ?: return null
            val selectedAccountTypes = payload.selectedAccounts.mapNotNull { it.type }.toSet()
            return if (selectedAccountTypes.size > 1) {
                // if user selected multiple different account types, present a special data access notice
                payload.multipleAccountTypesSelectedDataAccessNotice
            } else {
                // we get here if user selected:
                // 1) one account
                // 2) or, multiple accounts of the same account type
                payload.selectedAccounts.firstOrNull()?.display?.dataAccessNotice
                    // if no account was selected, use the consent
                    ?: payload.defaultDataAccessNotice
            }
        }

    val cta: TextResource
        get() {
            val payload = payload()

            return if (payload?.singleAccount == true) {
                val selectedAccount = payload.selectedAccounts.singleOrNull()?.display

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

internal data class LinkedAccount(
    val account: PartnerAccount,
    val display: NetworkedAccount
) {
    // Bank accounts can have multiple types, determined by their prefixes.
    // (ex. linked account "bctmacct", manual account "csmrbankacct").
    val type: String?
        get() = account.id.split("_").firstOrNull()
}

private fun FinancialConnectionsGenericInfoScreen.withIcon(iconUrl: String?) = copy(
    header = header?.copy(icon = Image(default = iconUrl)),
)
