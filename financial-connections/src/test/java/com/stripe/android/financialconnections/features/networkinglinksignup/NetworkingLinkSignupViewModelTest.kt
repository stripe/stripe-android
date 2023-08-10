package com.stripe.android.financialconnections.features.networkinglinksignup

import com.airbnb.mvrx.test.MavericksTestRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.LookupAccount
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.domain.SynchronizeFinancialConnectionsSession
import com.stripe.android.financialconnections.model.NetworkingLinkSignupBody
import com.stripe.android.financialconnections.model.NetworkingLinkSignupPane
import com.stripe.android.financialconnections.model.TextUpdate
import com.stripe.android.financialconnections.repository.SaveToLinkWithStripeSucceededRepository
import com.stripe.android.financialconnections.utils.TestNavigationManager
import com.stripe.android.financialconnections.utils.UriUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkingLinkSignupViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mavericksTestRule = MavericksTestRule(testDispatcher = testDispatcher)

    private val getManifest = mock<GetManifest>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val getAuthorizationSessionAccounts = mock<GetCachedAccounts>()
    private val navigationManager = TestNavigationManager()
    private val lookupAccount = mock<LookupAccount>()
    private val saveAccountToLink = mock<SaveAccountToLink>()
    private val sync = mock<SynchronizeFinancialConnectionsSession>()
    private val saveToLinkWithStripeSucceeded = mock<SaveToLinkWithStripeSucceededRepository>()

    private fun buildViewModel(
        state: NetworkingLinkSignupState
    ) = NetworkingLinkSignupViewModel(
        getManifest = getManifest,
        logger = Logger.noop(),
        eventTracker = eventTracker,
        initialState = state,
        navigationManager = navigationManager,
        getCachedAccounts = getAuthorizationSessionAccounts,
        lookupAccount = lookupAccount,
        uriUtils = UriUtils(Logger.noop(), eventTracker),
        sync = sync,
        saveToLinkWithStripeSucceeded = saveToLinkWithStripeSucceeded,
        saveAccountToLink = saveAccountToLink
    )

    @Test
    fun `init - creates controllers with prefilled account holder email`() = runTest {
        val manifest = ApiKeyFixtures.sessionManifest().copy(
            businessName = "Business",
            accountholderCustomerEmailAddress = "test@test.com"
        )
        whenever(sync()).thenReturn(
            syncResponse().copy(
                text = TextUpdate(
                    consent = null,
                    networkingLinkSignupPane = networkingLinkSignupPane()
                )
            )
        )
        whenever(getManifest()).thenReturn(manifest)

        val viewModel = buildViewModel(NetworkingLinkSignupState())
        val state = viewModel.awaitState()
        val payload = requireNotNull(state.payload())
        assertThat(payload.emailController.fieldValue.first()).isEqualTo("test@test.com")
    }

    private fun networkingLinkSignupPane() = NetworkingLinkSignupPane(
        aboveCta = "Above CTA",
        body = NetworkingLinkSignupBody(emptyList()),
        cta = "CTA",
        skipCta = "Skip CTA",
        title = "Title"
    )
}
