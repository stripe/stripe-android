package com.stripe.android.financialconnections.features.accountpicker

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures.authorizationSession
import com.stripe.android.financialconnections.ApiKeyFixtures.cachedConsumerSession
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccount
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccountList
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.CachedPartnerAccount
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionAccounts
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.domain.SelectAccounts
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.presentation.withState
import com.stripe.android.financialconnections.repository.CachedConsumerSession
import com.stripe.android.financialconnections.utils.TestNavigationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
internal class AccountPickerViewModelTest {

    @get:Rule
    val testRule = CoroutineTestRule()

    private val pollAuthorizationSessionAccounts = mock<PollAuthorizationSessionAccounts>()
    private val getSync = mock<GetOrFetchSync>()
    private val navigationManager = TestNavigationManager()
    private val selectAccounts = mock<SelectAccounts>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val nativeAuthFlowCoordinator = NativeAuthFlowCoordinator()
    private val saveAccountToLink = mock<SaveAccountToLink>()

    private fun buildViewModel(
        state: AccountPickerState
    ) = AccountPickerViewModel(
        initialState = state,
        eventTracker = eventTracker,
        selectAccounts = selectAccounts,
        getOrFetchSync = getSync,
        navigationManager = navigationManager,
        logger = Logger.noop(),
        handleClickableUrl = mock(),
        pollAuthorizationSessionAccounts = pollAuthorizationSessionAccounts,
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        saveAccountToLink = saveAccountToLink,
        consumerSessionProvider = { cachedConsumerSession() },
        presentSheet = mock(),
    )

    @Test
    fun `init - if PartnerAccounts response returns skipAccountSelection, state includes it`() =
        runTest {
            givenManifestReturns(
                sessionManifest().copy(
                    activeAuthSession = authorizationSession().copy(
                        skipAccountSelection = null
                    )
                )
            )
            givenPollAccountsReturns(
                partnerAccountList().copy(
                    skipAccountSelection = true
                )
            )

            val viewModel = buildViewModel(AccountPickerState())

            withState(viewModel) { state ->
                assertEquals(state.payload()!!.skipAccountSelection, true)
            }
        }

    @Test
    fun `init - if AuthSession returns skipAccountSelection, state includes it`() = runTest {
        givenManifestReturns(
            sessionManifest().copy(
                activeAuthSession = authorizationSession().copy(
                    skipAccountSelection = true
                )
            )
        )

        givenPollAccountsReturns(
            partnerAccountList().copy(
                skipAccountSelection = null
            )
        )

        val viewModel = buildViewModel(AccountPickerState())

        withState(viewModel) { state ->
            val payload = state.payload()!!
            assertEquals(payload.skipAccountSelection, true)
            assertEquals(payload.shouldSkipPane, true)
        }
    }

    @Test
    fun `init - if AuthSession returns institutionSkipAccountSelection and singleAccount, state includes it`() =
        runTest {
            givenManifestReturns(
                sessionManifest().copy(
                    singleAccount = true,
                    activeAuthSession = authorizationSession().copy(
                        institutionSkipAccountSelection = true,
                    )
                )
            )

            givenPollAccountsReturns(
                partnerAccountList().copy(
                    data = listOf(partnerAccount())
                )
            )

            val viewModel = buildViewModel(AccountPickerState())

            withState(viewModel) { state ->
                assertEquals(state.payload()!!.userSelectedSingleAccountInInstitution, true)
                assertEquals(state.payload()!!.shouldSkipPane, true)
            }
        }

    @Test
    fun `init - if singleAccount, pre-select first available account`() = runTest {
        givenManifestReturns(
            sessionManifest().copy(
                singleAccount = true,
                activeAuthSession = authorizationSession()
            )
        )

        givenPollAccountsReturns(
            partnerAccountList().copy(
                data = listOf(
                    partnerAccount().copy(id = "unelectable", _allowSelection = false),
                    partnerAccount().copy(id = "selectable")
                )
            )
        )

        val viewModel = buildViewModel(AccountPickerState())

        withState(viewModel) { state ->
            assertThat(state.selectedIds).isEqualTo(setOf("selectable"))
        }

        eventTracker.assertContainsEvent(
            "linked_accounts.account_picker.accounts_auto_selected",
            mapOf(
                "account_ids" to "selectable",
                "is_single_account" to "true",
            )
        )
    }

    @Test
    fun `init - if not singleAccount, pre-selects all accounts`() = runTest {
        givenManifestReturns(
            sessionManifest().copy(
                singleAccount = false,
                activeAuthSession = authorizationSession()
            )
        )

        givenPollAccountsReturns(
            partnerAccountList().copy(
                data = listOf(
                    partnerAccount().copy(id = "unelectable", _allowSelection = false),
                    partnerAccount().copy(id = "selectable_1"),
                    partnerAccount().copy(id = "selectable_2")
                )
            )
        )

        val viewModel = buildViewModel(AccountPickerState())

        withState(viewModel) { state ->
            assertThat(state.selectedIds).isEqualTo(setOf("selectable_1", "selectable_2"))
        }

        eventTracker.assertContainsEvent(
            "linked_accounts.account_picker.accounts_auto_selected",
            mapOf(
                "account_ids" to "selectable_1 selectable_2",
                "is_single_account" to "false",
            )
        )
    }

    @Test
    fun `Does not save selected accounts to Link in the payment flow`() = runTest {
        val accounts = partnerAccountList("id_1", "id2").copy(
            nextPane = Pane.ATTACH_LINKED_PAYMENT_ACCOUNT,
        )

        givenManifestReturns(
            sessionManifest().copy(
                singleAccount = false,
                activeAuthSession = authorizationSession(),
                paymentMethodType = FinancialConnectionsAccount.SupportedPaymentMethodTypes.US_BANK_ACCOUNT,
            )
        )

        givenPollAccountsReturns(accounts)
        givenSelectAccountsReturns(accounts)

        val viewModel = buildViewModel(AccountPickerState())
        viewModel.onSubmit()

        verify(saveAccountToLink, never()).existing(
            consumerSessionClientSecret = any(),
            selectedAccounts = any(),
            shouldPollAccountNumbers = any(),
        )

        navigationManager.assertNavigatedTo(
            destination = Pane.ATTACH_LINKED_PAYMENT_ACCOUNT.destination,
            pane = Pane.ACCOUNT_PICKER,
            popUpTo = null,
        )
    }

    @Test
    fun `Does not save selected accounts to Link if we don't have a consumer session`() = runTest {
        val accounts = partnerAccountList("id_1", "id2").copy(
            nextPane = Pane.SUCCESS,
        )

        givenManifestReturns(
            sessionManifest().copy(
                singleAccount = false,
                activeAuthSession = authorizationSession(),
            )
        )

        givenPollAccountsReturns(accounts)
        givenSelectAccountsReturns(accounts)

        val viewModel = buildViewModel(AccountPickerState())
        viewModel.onSubmit()

        verify(saveAccountToLink, never()).existing(
            consumerSessionClientSecret = any(),
            selectedAccounts = any(),
            shouldPollAccountNumbers = any(),
        )

        navigationManager.assertNavigatedTo(
            destination = Pane.SUCCESS.destination,
            pane = Pane.ACCOUNT_PICKER,
            popUpTo = null,
        )
    }

    @Test
    fun `Saves selected accounts to Link if we have a consumer session`() = runTest {
        val consumerSession = CachedConsumerSession(
            clientSecret = "clientSecret",
            emailAddress = "test@test.com",
            phoneNumber = "(***) *** **12",
            publishableKey = null,
            isVerified = true,
        )
        val accounts = partnerAccountList("id_1", "id2").copy(
            nextPane = Pane.SUCCESS,
        )

        givenManifestReturns(
            sessionManifest().copy(
                singleAccount = false,
                activeAuthSession = authorizationSession(),
                accountholderIsLinkConsumer = true,
                isNetworkingUserFlow = true,
            )
        )

        givenPollAccountsReturns(accounts)
        givenSelectAccountsReturns(accounts)

        val viewModel = buildViewModel(AccountPickerState())
        viewModel.onSubmit()

        verify(saveAccountToLink).existing(
            consumerSessionClientSecret = consumerSession.clientSecret,
            selectedAccounts = accounts.data.map { CachedPartnerAccount(it.id, it.linkedAccountId) },
            shouldPollAccountNumbers = true,
        )

        navigationManager.assertNavigatedTo(
            destination = Pane.SUCCESS.destination,
            pane = Pane.ACCOUNT_PICKER,
            popUpTo = null,
        )
    }

    @Test
    fun `onEnterDetailsManually - navigates to manual entry`() = runTest {
        val viewModel = buildViewModel(AccountPickerState())

        viewModel.onEnterDetailsManually()

        navigationManager.assertNavigatedTo(
            destination = Pane.MANUAL_ENTRY.destination,
            pane = Pane.ACCOUNT_PICKER,
            popUpTo = null,
        )
    }

    private suspend fun givenManifestReturns(manifest: FinancialConnectionsSessionManifest) {
        whenever(getSync()).thenReturn(syncResponse(manifest))
    }

    private suspend fun givenPollAccountsReturns(
        response: PartnerAccountsList
    ) {
        whenever(
            pollAuthorizationSessionAccounts(
                canRetry = any(),
                sync = any()
            )
        ).thenReturn(response)
    }

    private suspend fun givenSelectAccountsReturns(
        response: PartnerAccountsList
    ) {
        whenever(
            selectAccounts(
                selectedAccountIds = any(),
                sessionId = any(),
                updateLocalCache = any(),
            )
        ).thenReturn(response)
    }
}
