package com.stripe.android.financialconnections.features.networkinglinksignup

import com.airbnb.mvrx.test.MavericksTestRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.LookupAccount
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.repository.SaveToLinkWithStripeSucceededRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class NetworkingLinkSignupViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mavericksTestRule = MavericksTestRule(testDispatcher = testDispatcher)

    private val getManifest = mock<GetManifest>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val getAuthorizationSessionAccounts = mock<GetCachedAccounts>()
    private val goNext = mock<GoNext>()
    private val lookupAccount = mock<LookupAccount>()
    private val saveAccountToLink = mock<SaveAccountToLink>()
    private val saveToLinkWithStripeSucceeded = mock<SaveToLinkWithStripeSucceededRepository>()

    private fun buildViewModel(
        state: NetworkingLinkSignupState
    ) = NetworkingLinkSignupViewModel(
        getManifest = getManifest,
        logger = Logger.noop(),
        eventTracker = eventTracker,
        initialState = state,
        goNext = goNext,
        getCachedAccounts = getAuthorizationSessionAccounts,
        lookupAccount = lookupAccount,
        saveToLinkWithStripeSucceeded = saveToLinkWithStripeSucceeded,
        saveAccountToLink = saveAccountToLink
    )

    @Test
    fun `init - creates controllers with prefilled account holder email`() = runTest {
        val manifest = ApiKeyFixtures.sessionManifest().copy(
            businessName = "Business",
            accountholderCustomerEmailAddress = "test@test.com"
        )
        whenever(getManifest()).thenReturn(manifest)

        val viewModel = buildViewModel(NetworkingLinkSignupState())
        val state = viewModel.awaitState()
        val payload = requireNotNull(state.payload())
        assertThat(payload.emailController.fieldValue.first()).isEqualTo("test@test.com")
    }
}
