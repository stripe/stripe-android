package com.stripe.android.financialconnections.features.accountpicker

import com.airbnb.mvrx.test.MavericksTestRule
import com.airbnb.mvrx.withState
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccount
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccountList
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
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
    private val getManifest = mock<GetManifest>()
    private val goNext = mock<GoNext>()
    private val navigationManager = mock<NavigationManager>()
    private val selectAccounts = mock<SelectAccounts>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()

    private fun buildViewModel(
        state: AccountPickerState
    ) = AccountPickerViewModel(
        initialState = state,
        eventTracker = eventTracker,
        selectAccounts = selectAccounts,
        getManifest = getManifest,
        goNext = goNext,
        navigationManager = navigationManager,
        logger = Logger.noop(),
        pollAuthorizationSessionAccounts = pollAuthorizationSessionAccounts
    )

    @Test
    fun `init - if PartnerAccounts response returns skipAccountSelection, state includes it`() =
        runTest {
            givenManifestReturns(
                ApiKeyFixtures.sessionManifest().copy(
                    activeAuthSession = ApiKeyFixtures.authorizationSession().copy(
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
            ApiKeyFixtures.sessionManifest().copy(
                activeAuthSession = ApiKeyFixtures.authorizationSession().copy(
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
                ApiKeyFixtures.sessionManifest().copy(
                    singleAccount = true,
                    activeAuthSession = ApiKeyFixtures.authorizationSession().copy(
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

    private suspend fun givenManifestReturns(manifest: FinancialConnectionsSessionManifest) {
        whenever(getManifest()).thenReturn(manifest)
    }

    private suspend fun givenPollAccountsReturns(
        response: PartnerAccountsList
    ) {
        whenever(
            pollAuthorizationSessionAccounts(
                canRetry = any(),
                manifest = any()
            )
        ).thenReturn(response)
    }
}
