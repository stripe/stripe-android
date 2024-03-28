package com.stripe.android.financialconnections.features.linkaccountpicker

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent
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
import com.stripe.android.financialconnections.domain.UpdateLocalManifest
import com.stripe.android.financialconnections.features.common.MerchantDataAccessModel
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerClickableText.DATA
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerState.BottomSheetContent
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerState.BottomSheetContent.UpdateRequired
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.AddNewAccount
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.BANK_AUTH_REPAIR
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.PARTNER_AUTH
import com.stripe.android.financialconnections.model.NetworkedAccount
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.repository.CoreAuthorizationPendingNetworkingRepairRepository
import com.stripe.android.financialconnections.ui.HandleClickableUrl
import com.stripe.android.financialconnections.ui.TextResource
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

internal class LinkAccountPickerViewModel @Inject constructor(
    initialState: LinkAccountPickerState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getCachedConsumerSession: GetCachedConsumerSession,
    private val handleClickableUrl: HandleClickableUrl,
    private val fetchNetworkedAccounts: FetchNetworkedAccounts,
    private val selectNetworkedAccounts: SelectNetworkedAccounts,
    private val updateLocalManifest: UpdateLocalManifest,
    private val updateCachedAccounts: UpdateCachedAccounts,
    private val coreAuthorizationPendingNetworkingRepair: CoreAuthorizationPendingNetworkingRepairRepository,
    private val getSync: GetOrFetchSync,
    private val navigationManager: NavigationManager,
    private val logger: Logger
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
                    setState {
                        val bottomSheetContent = payload()?.dataAccessNotice?.let {
                            BottomSheetContent.Notice(it)
                        }
                        copy(bottomSheetContent = bottomSheetContent)
                    }
                }
            )
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

            // We assume that at this point, all selected accounts have the same next pane.
            // Otherwise, the user would have been presented with an update-required bottom
            // sheet before.
            val nextPane = accounts.firstOrNull()?.nextPaneOnSelection
            val accountIds = accounts.map { it.id }.toSet()

            updateCachedAccounts { accounts }

            eventTracker.track(
                FinancialConnectionsAnalyticsEvent.AccountsSubmitted(
                    accountIds = accountIds,
                    isSkipAccountSelection = false,
                    pane = PANE
                )
            )

            if (nextPane == Pane.SUCCESS) {
                selectNetworkedAccounts(
                    consumerSessionClientSecret = payload.consumerSessionClientSecret,
                    selectedAccountIds = accountIds,
                )
                updateLocalManifest { it.copy(activeInstitution = null) }
                eventTracker.track(Click("click.link_accounts", PANE))
            }

            nextPane?.let {
                FinancialConnections.emitEvent(name = Name.ACCOUNTS_SELECTED)
                navigationManager.tryNavigateTo(it.destination(referrer = PANE))
            }

            Unit
        }.execute { copy(selectNetworkedAccountAsync = it) }
    }

    fun onUpdateAccount(updateRequired: UpdateRequired) {
        onBottomSheetDismiss()

        viewModelScope.launch {
            when (val type = updateRequired.type) {
                is UpdateRequired.Type.Repair -> {
                    coreAuthorizationPendingNetworkingRepair.set(type.authorization)
                    eventTracker.track(Click("click.repair_accounts", PANE))
                }
                is UpdateRequired.Type.PartnerAuth -> {
                    updateLocalManifest {
                        it.copy(activeInstitution = type.institution)
                    }
                }
            }

            navigationManager.tryNavigateTo(updateRequired.type.pane.destination(referrer = PANE))
        }
    }

    fun onBottomSheetDismiss() {
        setState { copy(bottomSheetContent = null) }
    }

    fun onAccountClick(partnerAccount: PartnerAccount) {
        setState {
            val updateRequired = createUpdateRequiredContent(
                partnerAccount = partnerAccount,
                partnerToCoreAuths = payload()?.partnerToCoreAuths,
            )

            if (updateRequired != null) {
                copy(bottomSheetContent = updateRequired)
            } else {
                val payload = requireNotNull(payload())
                copy(
                    selectedAccountIds = when {
                        payload.singleAccount -> setOf(partnerAccount.id)
                        partnerAccount.id in selectedAccountIds -> selectedAccountIds - partnerAccount.id
                        else -> selectedAccountIds + partnerAccount.id
                    }
                )
            }
        }
    }

    fun onViewEffectLaunched() {
        setState { copy(viewEffect = null) }
    }

    companion object {

        internal val PANE = Pane.LINK_ACCOUNT_PICKER

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent
                        .linkAccountPickerSubcomponent
                        .create(LinkAccountPickerState())
                        .viewModel
                }
            }
    }
}

internal data class LinkAccountPickerState(
    val payload: Async<Payload> = Uninitialized,
    val selectNetworkedAccountAsync: Async<Unit> = Uninitialized,
    val bottomSheetContent: BottomSheetContent? = null,
    val selectedAccountIds: Set<String> = emptySet(),
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

        fun selectedPartnerAccounts(selected: Set<String>): List<PartnerAccount> {
            return accounts.filter { it.first.id in selected }.map { it.first }
        }
    }

    sealed interface BottomSheetContent {

        data class UpdateRequired(
            val iconUrl: String?,
            val type: Type,
        ) : BottomSheetContent {

            sealed class Type(val pane: Pane) {
                data class Repair(val authorization: String) : Type(BANK_AUTH_REPAIR)
                data class PartnerAuth(val institution: FinancialConnectionsInstitution) : Type(PARTNER_AUTH)
            }
        }

        data class Notice(
            val content: DataAccessNotice,
        ) : BottomSheetContent
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
                TextResource.PluralId(
                    value = R.plurals.stripe_account_picker_cta_link,
                    count = selectedAccountIds.size,
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
): UpdateRequired? {
    return when (partnerAccount.nextPaneOnSelection) {
        BANK_AUTH_REPAIR -> {
            UpdateRequired(
                iconUrl = partnerAccount.institution?.icon?.default,
                type = UpdateRequired.Type.Repair(
                    partnerToCoreAuths!!.getValue(partnerAccount.authorization)
                ),
            )
        }
        PARTNER_AUTH -> {
            UpdateRequired(
                iconUrl = partnerAccount.institution?.icon?.default,
                type = UpdateRequired.Type.PartnerAuth(
                    institution = partnerAccount.institution!!,
                ),
            )
        }
        else -> {
            null
        }
    }
}
