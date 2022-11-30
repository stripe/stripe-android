package com.stripe.android.financialconnections.features.accountpicker

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ClickLinkAccounts
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Error
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PollAccountsSucceeded
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionAccounts
import com.stripe.android.financialconnections.domain.SelectAccounts
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerState.SelectionMode
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.features.consent.ConsentTextBuilder
import com.stripe.android.financialconnections.features.consent.FinancialConnectionsUrlResolver
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.utils.measureTimeMillis
import com.stripe.android.uicore.format.CurrencyFormatter
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
internal class AccountPickerViewModel @Inject constructor(
    initialState: AccountPickerState,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val selectAccounts: SelectAccounts,
    private val getManifest: GetManifest,
    private val goNext: GoNext,
    private val locale: Locale?,
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
            val manifest = getManifest()
            val activeInstitution = manifest.activeInstitution
            val activeAuthSession = requireNotNull(manifest.activeAuthSession)
            val (partnerAccountList, millis) = measureTimeMillis {
                pollAuthorizationSessionAccounts(
                    manifest = manifest,
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
            val accounts = partnerAccountList.data.map { account ->
                AccountPickerState.PartnerAccountUI(
                    account = account,
                    institutionIcon = activeInstitution?.icon?.default,
                    formattedBalance =
                    if (account.balanceAmount != null && account.currency != null) {
                        CurrencyFormatter
                            .format(
                                amount = account.balanceAmount.toLong(),
                                amountCurrencyCode = account.currency,
                                targetLocale = locale ?: Locale.getDefault()
                            )
                    } else {
                        null
                    }
                )
            }.sortedBy { it.account.allowSelection.not() }

            AccountPickerState.Payload(
                skipAccountSelection = activeAuthSession.skipAccountSelection == true,
                accounts = accounts,
                selectionMode = if (manifest.singleAccount) SelectionMode.RADIO else SelectionMode.CHECKBOXES,
                accessibleData = AccessibleDataCalloutModel(
                    businessName = ConsentTextBuilder.getBusinessName(manifest),
                    permissions = manifest.permissions,
                    isStripeDirect = manifest.isStripeDirect ?: false,
                    dataPolicyUrl = FinancialConnectionsUrlResolver.getDataPolicyUrl(manifest)
                ),
                singleAccount = manifest.singleAccount,
                institutionSkipAccountSelection = activeAuthSession.institutionSkipAccountSelection == true,
                businessName = manifest.businessName,
                stripeDirect = manifest.isStripeDirect ?: false
            ).also {
                eventTracker.track(PaneLoaded(Pane.ACCOUNT_PICKER))
            }
        }.execute { copy(payload = it) }
    }

    private fun onPayloadLoaded() {
        onAsync(AccountPickerState::payload, onSuccess = { payload ->
            when {
                // If account selection has to be skipped, submit all selectable accounts.
                payload.skipAccountSelection -> submitAccounts(
                    selectedIds = payload.selectableAccounts.map { it.account.id }.toSet(),
                    updateLocalCache = false
                )
                // the user saw an OAuth account selection screen and selected
                // just one to send back in a single-account context. treat these as if
                // we had done account selection, and submit.
                payload.singleAccount &&
                    payload.institutionSkipAccountSelection &&
                    payload.accounts.size == 1 -> submitAccounts(
                    selectedIds = setOf(payload.accounts.first().account.id),
                    updateLocalCache = true
                )
            }
        })
    }

    private fun logErrors() {
        onAsync(
            AccountPickerState::payload,
            onFail = {
                eventTracker.track(Error(Pane.ACCOUNT_PICKER, it))
                logger.error("Error retrieving accounts", it)
            },
        )
        onAsync(
            AccountPickerState::selectAccounts,
            onFail = {
                eventTracker.track(Error(Pane.ACCOUNT_PICKER, it))
                logger.error("Error selecting accounts", it)
            }
        )
    }

    fun onAccountClicked(account: PartnerAccount) {
        withState { state ->
            state.payload()?.let { payload ->
                when (payload.selectionMode) {
                    SelectionMode.RADIO -> setState {
                        copy(selectedIds = setOf(account.id))
                    }

                    SelectionMode.CHECKBOXES -> if (state.selectedIds.contains(account.id)) {
                        setState {
                            copy(selectedIds = selectedIds - account.id)
                        }
                    } else {
                        setState {
                            copy(selectedIds = selectedIds + account.id)
                        }
                    }
                }
            } ?: run {
                logger.error("account clicked without available payload.")
            }
        }
    }

    fun onSubmit() {
        viewModelScope.launch {
            eventTracker.track(ClickLinkAccounts(Pane.ACCOUNT_PICKER))
        }
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
            val manifest = getManifest()
            val accountsList: PartnerAccountsList = selectAccounts(
                selectedAccountIds = selectedIds,
                sessionId = requireNotNull(manifest.activeAuthSession).id,
                updateLocalCache = updateLocalCache
            )
            goNext(accountsList.nextPane)
            accountsList
        }.execute {
            copy(selectAccounts = it)
        }
    }

    fun selectAnotherBank() = navigationManager.navigate(NavigationDirections.reset)

    fun onEnterDetailsManually() = navigationManager.navigate(NavigationDirections.manualEntry)

    fun onLoadAccountsAgain() {
        setState { copy(canRetry = false) }
        loadAccounts()
    }

    fun onSelectAllAccountsClicked() {
        withState { state ->
            state.payload()?.let { payload ->
                if (state.allAccountsSelected) {
                    // unselect all accounts
                    setState { copy(selectedIds = emptySet()) }
                } else {
                    // select all accounts
                    setState {
                        val ids = payload.selectableAccounts.map { it.account.id }.toSet()
                        copy(selectedIds = ids)
                    }
                }
            }
        }
    }

    fun onLearnMoreAboutDataAccessClick() {
        viewModelScope.launch {
            eventTracker.track(
                FinancialConnectionsEvent.ClickLearnMoreDataAccess(Pane.ACCOUNT_PICKER)
            )
        }
    }

    companion object :
        MavericksViewModelFactory<AccountPickerViewModel, AccountPickerState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: AccountPickerState
        ): AccountPickerViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel!!
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
    val canRetry: Boolean = true,
    val selectAccounts: Async<PartnerAccountsList> = Uninitialized,
    val selectedIds: Set<String> = emptySet(),
) : MavericksState {

    val submitLoading: Boolean
        get() = payload is Loading || selectAccounts is Loading

    val submitEnabled: Boolean
        get() = selectedIds.isNotEmpty()

    val allAccountsSelected: Boolean
        get() = payload()?.selectableAccounts?.count() == selectedIds.count()

    data class Payload(
        val skipAccountSelection: Boolean,
        val accounts: List<PartnerAccountUI>,
        val selectionMode: SelectionMode,
        val accessibleData: AccessibleDataCalloutModel,
        val singleAccount: Boolean,
        val stripeDirect: Boolean,
        val businessName: String?,
        val institutionSkipAccountSelection: Boolean
    ) {

        val selectableAccounts
            get() = accounts.filter { it.account.allowSelection }

        val subtitle: TextResource?
            get() = when {
                singleAccount.not() -> null
                stripeDirect -> TextResource.StringId(
                    R.string.stripe_accountpicker_singleaccount_description_withstripe
                )

                businessName != null -> TextResource.StringId(
                    R.string.stripe_accountpicker_singleaccount_description,
                    listOf(businessName)
                )

                else -> TextResource.StringId(
                    R.string.stripe_accountpicker_singleaccount_description_nobusinessname
                )
            }
    }

    data class PartnerAccountUI(
        val account: PartnerAccount,
        val institutionIcon: String?,
        val formattedBalance: String?,
    )

    enum class SelectionMode {
        RADIO, CHECKBOXES
    }
}
