package com.stripe.android.financialconnections.features.accountpicker

import com.airbnb.mvrx.test.MavericksTestRule
import com.airbnb.mvrx.withState
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures.authorizationSession
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccount
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccountList
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionAccounts
import com.stripe.android.financialconnections.domain.SelectAccounts
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.navigation.NavigationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
internal class AccountPickerViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule()

    private val pollAuthorizationSessionAccounts = mock<PollAuthorizationSessionAccounts>()
    private val getSync = mock<GetOrFetchSync>()
    private val navigationManager = mock<NavigationManager>()
    private val selectAccounts = mock<SelectAccounts>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()

    private fun buildViewModel(
        state: AccountPickerState
    ) = AccountPickerViewModel(
        initialState = state,
        eventTracker = eventTracker,
        selectAccounts = selectAccounts,
        getOrFetchSync = getSync,
        navigationManager = navigationManager,
        logger = Logger.noop(),
        pollAuthorizationSessionAccounts = pollAuthorizationSessionAccounts
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
}
