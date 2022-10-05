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
import com.stripe.android.financialconnections.ui.TextResource
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
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
        onPayloadLoaded()
        loadAccounts()
    }

    private fun loadAccounts() {
        suspend {
            val state = awaitState()
            val manifest = getManifest()
            val partnerAccountList = pollAuthorizationSessionAccounts(
                manifest = manifest,
                canRetry = state.canRetry
            )
            val accounts = partnerAccountList.data.map { account ->
                AccountPickerState.PartnerAccountUI(
                    account = account,
                    enabled = account.enabled(manifest)
                )
            }.sortedBy { it.enabled }
            val (preselectedIds, selectionMode) = selectionConfig(accounts, manifest)
            val activeAuthSession = requireNotNull(manifest.activeAuthSession)
            AccountPickerState.Payload(
                skipAccountSelection = activeAuthSession.skipAccountSelection == true,
                accounts = accounts,
                selectionMode = selectionMode,
                accessibleData = AccessibleDataCalloutModel(
                    businessName = ConsentTextBuilder.getBusinessName(manifest),
                    permissions = manifest.permissions,
                    isStripeDirect = manifest.isStripeDirect ?: false,
                    dataPolicyUrl = FinancialConnectionsUrlResolver.getDataPolicyUrl(manifest)
                ),
                selectedIds = preselectedIds,
                singleAccount = manifest.singleAccount,
                institutionSkipAccountSelection = activeAuthSession.institutionSkipAccountSelection == true,
                businessName = manifest.businessName,
                stripeDirect = manifest.isStripeDirect ?: false
            )
        }.execute { copy(payload = it) }
    }

    private fun onPayloadLoaded() {
        onAsync(AccountPickerState::payload, onSuccess = { payload ->
            when {
                // Navigate to success right away instead of allowing the user to select accounts.
                payload.skipAccountSelection -> {
                    navigationManager.navigate(NavigationDirections.success)
                }
                // the user saw an OAuth account selection screen and selected
                // just one to send back in a single-account context. treat these as if
                // we had done account selection, and submit.
                payload.singleAccount &&
                    payload.institutionSkipAccountSelection &&
                    payload.accounts.size == 1 -> {
                    submitAccounts(selectedIds = setOf(payload.accounts.first().account.id))
                }
            }
        })
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
                manifest.activeAuthSession?.institutionSkipAccountSelection == true &&
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

    fun onSubmit() {
        withState { state -> submitAccounts(state.selectedIds) }
    }

    private fun submitAccounts(
        selectedIds: Set<String>
    ) {
        suspend {
            val manifest = getManifest()
            val accountsList: PartnerAccountsList = selectAccounts(
                selectedAccountIds = selectedIds,
                sessionId = manifest.activeAuthSession!!.id
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
    val canRetry: Boolean = true,
    val selectAccounts: Async<PartnerAccountsList> = Uninitialized,
    val selectedIds: Set<String> = emptySet()
) : MavericksState {

    val submitLoading: Boolean
        get() = payload is Loading || selectAccounts is Loading

    val submitEnabled: Boolean
        get() = selectedIds.isNotEmpty()

    data class Payload(
        val skipAccountSelection: Boolean,
        val accounts: List<PartnerAccountUI>,
        val selectionMode: SelectionMode,
        val accessibleData: AccessibleDataCalloutModel,
        val selectedIds: Set<String>,
        val singleAccount: Boolean,
        val stripeDirect: Boolean,
        val businessName: String?,
        val institutionSkipAccountSelection: Boolean
    ) {

        val subtitle: TextResource?
            get() = when {
                selectionMode != SelectionMode.DROPDOWN || singleAccount.not() -> null
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
        val enabled: Boolean
    )

    enum class SelectionMode {
        DROPDOWN, RADIO, CHECKBOXES
    }
}
