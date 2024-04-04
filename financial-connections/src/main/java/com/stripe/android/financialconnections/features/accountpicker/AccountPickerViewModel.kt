package com.stripe.android.financialconnections.features.accountpicker

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.AccountSelected
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.AccountsAutoSelected
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.AccountsSubmitted
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ClickLearnMoreDataAccess
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ClickLinkAccounts
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PollAccountsSucceeded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.GetCachedConsumerSession
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionAccounts
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.domain.SelectAccounts
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerClickableText.DATA
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.SelectionMode
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.ViewEffect
import com.stripe.android.financialconnections.features.common.MerchantDataAccessModel
import com.stripe.android.financialconnections.features.common.canSaveAccountsToLink
import com.stripe.android.financialconnections.features.common.isDataFlow
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.DataAccess
import com.stripe.android.financialconnections.features.notice.PresentNoticeSheet
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.navigation.Destination.ManualEntry
import com.stripe.android.financialconnections.navigation.Destination.Reset
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.ui.HandleClickableUrl
import com.stripe.android.financialconnections.utils.error
import com.stripe.android.financialconnections.utils.measureTimeMillis
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

internal class AccountPickerViewModel @Inject constructor(
    initialState: AccountPickerState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getCachedConsumerSession: GetCachedConsumerSession,
    private val saveAccountToLink: SaveAccountToLink,
    private val selectAccounts: SelectAccounts,
    private val getOrFetchSync: GetOrFetchSync,
    private val navigationManager: NavigationManager,
    private val handleClickableUrl: HandleClickableUrl,
    private val logger: Logger,
    private val pollAuthorizationSessionAccounts: PollAuthorizationSessionAccounts,
    private val presentNoticeSheet: PresentNoticeSheet,
) : FinancialConnectionsViewModel<AccountPickerState>(initialState, nativeAuthFlowCoordinator) {

    init {
        logErrors()
        onPayloadLoaded()
        loadAccounts()
    }

    override fun updateTopAppBar(state: AccountPickerState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = PANE,
            allowBackNavigation = false,
            error = state.payload.error,
        )
    }

    private fun loadAccounts() {
        suspend {
            val state = stateFlow.value
            val sync = getOrFetchSync()
            val dataAccessNotice = sync.text?.consent?.dataAccessNotice
            val manifest = sync.manifest
            val activeAuthSession = requireNotNull(manifest.activeAuthSession)
            val (partnerAccountList, millis) = measureTimeMillis {
                pollAuthorizationSessionAccounts(
                    sync = sync,
                    canRetry = state.canRetry
                )
            }
            if (partnerAccountList.data.isNotEmpty()) {
                eventTracker.track(
                    PollAccountsSucceeded(
                        authSessionId = activeAuthSession.id,
                        duration = millis
                    )
                )
            }
            val accounts = partnerAccountList.data.sortedBy { it.allowSelection.not() }

            AccountPickerState.Payload(
                // note that this uses ?? instead of ||, we do NOT want to skip account selection
                // if EITHER of these are true, we only want to skip account selection when
                //  `accountsPayload.skipAccountSelection` is true, OR `accountsPayload.skipAccountSelection` nil
                // AND `authSession.skipAccountSelection` is true
                skipAccountSelection = partnerAccountList.skipAccountSelection
                    ?: activeAuthSession.skipAccountSelection
                    ?: false,
                accounts = accounts,
                selectionMode = if (manifest.singleAccount) SelectionMode.Single else SelectionMode.Multiple,
                dataAccessNotice = dataAccessNotice,
                merchantDataAccess = MerchantDataAccessModel(
                    businessName = manifest.businessName,
                    permissions = manifest.permissions,
                    isStripeDirect = manifest.isStripeDirect ?: false
                ),
                singleAccount = manifest.singleAccount,
                userSelectedSingleAccountInInstitution = manifest.singleAccount &&
                    activeAuthSession.institutionSkipAccountSelection == true &&
                    accounts.size == 1,
                businessName = manifest.businessName,
                stripeDirect = manifest.isStripeDirect ?: false
            ).also {
                eventTracker.track(PaneLoaded(PANE))
            }
        }.execute { copy(payload = it) }
    }

    private fun onPayloadLoaded() {
        onAsync(AccountPickerState::payload, onSuccess = { payload ->
            when {
                // If account selection has to be skipped, submit all selectable accounts.
                payload.skipAccountSelection -> submitAccounts(
                    selectedIds = payload.selectableAccounts.map { it.id }.toSet(),
                    updateLocalCache = false,
                    isSkipAccountSelection = true
                )
                // the user saw an OAuth account selection screen and selected
                // just one to send back in a single-account context. treat these as if
                // we had done account selection, and submit.
                payload.userSelectedSingleAccountInInstitution -> submitAccounts(
                    selectedIds = setOf(payload.accounts.first().id),
                    updateLocalCache = true,
                    isSkipAccountSelection = true
                )

                // Auto-select the first selectable account and log.
                payload.selectionMode == SelectionMode.Single -> {
                    val selectedId = setOfNotNull(payload.selectableAccounts.firstOrNull()?.id)
                    eventTracker.track(
                        AccountsAutoSelected(
                            isSingleAccount = true,
                            accountIds = selectedId
                        )
                    )
                    setState { copy(selectedIds = selectedId) }
                }

                // Auto-select all selectable accounts and log.
                payload.selectionMode == SelectionMode.Multiple -> {
                    val selectedIds = payload.selectableAccounts.map { it.id }.toSet()
                    eventTracker.track(
                        AccountsAutoSelected(
                            isSingleAccount = false,
                            accountIds = selectedIds
                        )
                    )
                    setState { copy(selectedIds = selectedIds) }
                }
            }
        })
    }

    private fun logErrors() {
        onAsync(
            AccountPickerState::payload,
            onFail = {
                eventTracker.logError(
                    logger = logger,
                    pane = PANE,
                    extraMessage = "Error retrieving accounts",
                    error = it
                )
            },
        )
        onAsync(
            AccountPickerState::selectAccounts,
            onFail = {
                eventTracker.logError(
                    logger = logger,
                    pane = PANE,
                    extraMessage = "Error selecting accounts",
                    error = it
                )
            }
        )
    }

    fun onAccountClicked(account: PartnerAccount) = withState { state ->
        state.payload()?.let { payload ->
            val selectedIds = state.selectedIds
            val newSelectedIds = when (payload.selectionMode) {
                SelectionMode.Single -> setOf(account.id)
                SelectionMode.Multiple -> if (selectedIds.contains(account.id)) {
                    selectedIds - account.id
                } else {
                    selectedIds + account.id
                }
            }
            setState { copy(selectedIds = newSelectedIds) }
            logAccountSelectionChanges(
                idsBefore = selectedIds,
                idsAfter = newSelectedIds,
                isSingleAccount = payload.singleAccount
            )
        } ?: run {
            logger.error("account clicked without available payload.")
        }
    }

    private fun logAccountSelectionChanges(
        idsBefore: Set<String>,
        idsAfter: Set<String>,
        isSingleAccount: Boolean
    ) {
        viewModelScope.launch {
            val newIds = idsAfter - idsBefore
            val removedIds = idsBefore - idsAfter
            if (newIds.size == 1) {
                eventTracker.track(
                    AccountSelected(
                        isSingleAccount = isSingleAccount,
                        selected = true,
                        accountId = newIds.first()
                    )
                )
            }
            if (removedIds.size == 1) {
                eventTracker.track(
                    AccountSelected(
                        isSingleAccount = isSingleAccount,
                        selected = false,
                        accountId = removedIds.first()
                    )
                )
            }
        }
    }

    fun onSubmit() {
        viewModelScope.launch {
            eventTracker.track(ClickLinkAccounts(PANE))
        }
        FinancialConnections.emitEvent(name = Name.ACCOUNTS_SELECTED)
        withState { state ->
            state.payload()?.let {
                submitAccounts(
                    selectedIds = state.selectedIds,
                    updateLocalCache = true,
                    isSkipAccountSelection = false
                )
            } ?: run {
                logger.error("account clicked without available payload.")
            }
        }
    }

    private fun submitAccounts(
        selectedIds: Set<String>,
        updateLocalCache: Boolean,
        isSkipAccountSelection: Boolean
    ) {
        suspend {
            eventTracker.track(
                AccountsSubmitted(
                    accountIds = selectedIds,
                    isSkipAccountSelection = isSkipAccountSelection,
                    pane = PANE
                )
            )
            val manifest = getOrFetchSync().manifest
            val accountsList: PartnerAccountsList = selectAccounts(
                selectedAccountIds = selectedIds,
                sessionId = requireNotNull(manifest.activeAuthSession).id,
                updateLocalCache = updateLocalCache
            )

            val consumerSessionClientSecret = getCachedConsumerSession()?.clientSecret

            if (manifest.isDataFlow && manifest.canSaveAccountsToLink && consumerSessionClientSecret != null) {
                // In the data flow, we save accounts to Link in this screen. In the payment flow,
                // it happens in the AttachPaymentScreen.
                saveAccountToLink.existing(
                    consumerSessionClientSecret = consumerSessionClientSecret,
                    selectedAccounts = accountsList.data,
                    shouldPollAccountNumbers = manifest.isDataFlow,
                )
            }

            navigationManager.tryNavigateTo(accountsList.nextPane.destination(referrer = PANE))
            accountsList
        }.execute {
            copy(selectAccounts = it)
        }
    }

    fun selectAnotherBank() =
        navigationManager.tryNavigateTo(Reset(referrer = PANE))

    fun onEnterDetailsManually() =
        navigationManager.tryNavigateTo(ManualEntry(referrer = PANE))

    fun onLoadAccountsAgain() {
        setState { copy(canRetry = false) }
        loadAccounts()
    }

    fun onClickableTextClick(uri: String) = viewModelScope.launch {
        val date = Date()
        handleClickableUrl(
            currentPane = PANE,
            uri = uri,
            onNetworkUrlClicked = {
                setState { copy(viewEffect = ViewEffect.OpenUrl(uri, date.time)) }
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

    fun onViewEffectLaunched() {
        setState { copy(viewEffect = null) }
    }

    companion object {

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent
                        .accountPickerSubcomponent
                        .create(AccountPickerState())
                        .viewModel
                }
            }

        private val PANE = Pane.ACCOUNT_PICKER
    }
}

internal data class AccountPickerState(
    val payload: Async<Payload> = Uninitialized,
    val canRetry: Boolean = true,
    val selectAccounts: Async<PartnerAccountsList> = Uninitialized,
    val selectedIds: Set<String> = emptySet(),
    val viewEffect: ViewEffect? = null
) {

    val submitLoading: Boolean
        get() = payload is Loading || selectAccounts is Loading

    val submitEnabled: Boolean
        get() = selectedIds.isNotEmpty()

    data class Payload(
        val skipAccountSelection: Boolean,
        val accounts: List<PartnerAccount>,
        val dataAccessNotice: DataAccessNotice?,
        val selectionMode: SelectionMode,
        val merchantDataAccess: MerchantDataAccessModel,
        val singleAccount: Boolean,
        val stripeDirect: Boolean,
        val businessName: String?,
        val userSelectedSingleAccountInInstitution: Boolean,
    ) {

        val selectableAccounts
            get() = accounts.filter { it.allowSelection }

        val shouldSkipPane: Boolean
            get() = skipAccountSelection || userSelectedSingleAccountInInstitution
    }

    enum class SelectionMode {
        Single, Multiple
    }

    sealed class ViewEffect {
        data class OpenUrl(
            val url: String,
            val id: Long
        ) : ViewEffect()
    }
}

internal enum class AccountPickerClickableText(val value: String) {
    DATA("stripe://data-access-notice"),
}
