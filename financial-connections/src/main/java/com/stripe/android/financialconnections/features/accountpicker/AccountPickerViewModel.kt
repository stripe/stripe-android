package com.stripe.android.financialconnections.features.accountpicker

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.AccountSelected
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ClickLearnMoreDataAccess
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ClickLinkAccounts
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PollAccountsSucceeded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionAccounts
import com.stripe.android.financialconnections.domain.SelectAccounts
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.SelectionMode
import com.stripe.android.financialconnections.features.common.MerchantDataAccessModel
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.navigation.Destination.ManualEntry
import com.stripe.android.financialconnections.navigation.Destination.Reset
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.utils.measureTimeMillis
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
internal class AccountPickerViewModel @Inject constructor(
    initialState: AccountPickerState,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val selectAccounts: SelectAccounts,
    private val getOrFetchSync: GetOrFetchSync,
    private val navigationManager: NavigationManager,
    private val logger: Logger,
    private val pollAuthorizationSessionAccounts: PollAuthorizationSessionAccounts
) : MavericksViewModel<AccountPickerState>(initialState) {

    init {
        logErrors()
        onPayloadLoaded()
        loadAccounts()
    }

    private fun loadAccounts() {
        suspend {
            val state = awaitState()
            val sync = getOrFetchSync()
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
                skipAccountSelection = partnerAccountList.skipAccountSelection == true ||
                    activeAuthSession.skipAccountSelection == true,
                accounts = accounts,
                selectionMode = if (manifest.singleAccount) SelectionMode.Single else SelectionMode.Multiple,
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
                    updateLocalCache = false
                )
                // the user saw an OAuth account selection screen and selected
                // just one to send back in a single-account context. treat these as if
                // we had done account selection, and submit.
                payload.userSelectedSingleAccountInInstitution -> submitAccounts(
                    selectedIds = setOf(payload.accounts.first().id),
                    updateLocalCache = true
                )

                // Auto-select the first selectable account.
                payload.selectionMode == SelectionMode.Single -> setState {
                    copy(
                        selectedIds = setOfNotNull(
                            payload.selectableAccounts.firstOrNull()?.id
                        )
                    )
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
                submitAccounts(state.selectedIds, updateLocalCache = true)
            } ?: run {
                logger.error("account clicked without available payload.")
            }
        }
    }

    private fun submitAccounts(
        selectedIds: Set<String>,
        updateLocalCache: Boolean
    ) {
        suspend {
            val manifest = getOrFetchSync().manifest
            val accountsList: PartnerAccountsList = selectAccounts(
                selectedAccountIds = selectedIds,
                sessionId = requireNotNull(manifest.activeAuthSession).id,
                updateLocalCache = updateLocalCache
            )
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

    fun onLearnMoreAboutDataAccessClick() {
        viewModelScope.launch {
            eventTracker.track(ClickLearnMoreDataAccess(Pane.ACCOUNT_PICKER))
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

        private val PANE = Pane.ACCOUNT_PICKER
    }
}

internal data class AccountPickerState(
    val payload: Async<Payload> = Uninitialized,
    val canRetry: Boolean = true,
    val selectAccounts: Async<PartnerAccountsList> = Uninitialized,
    val selectedIds: Set<String> = emptySet(),
) : MavericksState {

    val submitLoading: Boolean
        get() = payload is Loading || selectAccounts is Loading

    val submitEnabled: Boolean
        get() = selectedIds.isNotEmpty()

    data class Payload(
        val skipAccountSelection: Boolean,
        val accounts: List<PartnerAccount>,
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
}
