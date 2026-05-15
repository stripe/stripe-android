package com.stripe.android.financialconnections.features.networkinglinksignup

import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSession
import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSessionSignup
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.domain.CachedPartnerAccount
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.RequestIntegrityToken
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.financialconnections.utils.TestNavigationManager
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.LinkBrand
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals

class LinkSignupHandlerForNetworkingTest {

    private lateinit var handler: LinkSignupHandlerForNetworking
    private val consumerRepository = mock<FinancialConnectionsConsumerSessionRepository>()
    private val getOrFetchSync = mock<GetOrFetchSync>()
    private val getCachedAccounts = mock<GetCachedAccounts>()
    private val saveAccountToLink = mock<SaveAccountToLink>()
    private val requestIntegrityToken = mock<RequestIntegrityToken>()
    private val navigationManager = TestNavigationManager()
    private val eventTracker = mock<FinancialConnectionsAnalyticsTracker>()
    private val logger = mock<Logger>()

    @Before
    fun setUp() {
        handler = LinkSignupHandlerForNetworking(
            consumerRepository,
            getOrFetchSync,
            getCachedAccounts,
            requestIntegrityToken,
            saveAccountToLink,
            eventTracker,
            navigationManager,
            "applicationId",
            logger
        )
    }

    private val validPayload = NetworkingLinkSignupState.Payload(
        merchantName = "Mock Merchant",
        emailController = mock(),
        prefilledEmail = "test@example.com",
        sessionId = "fcsess_5678",
        appVerificationEnabled = true,
        phoneController = mock {
            whenever(it.getCountryCode()).thenReturn("US")
        },
        isInstantDebits = false,
        content = mock()
    )

    private val verifiedFlowState = NetworkingLinkSignupState(
        validEmail = "test@example.com",
        validPhone = "+1987654321",
        isInstantDebits = false,
        payload = Async.Success(validPayload)
    )

    private suspend fun setupVerifiedFlow(
        signup: ConsumerSessionSignup = consumerSessionSignup()
    ) {
        whenever(requestIntegrityToken(anyOrNull(), anyOrNull())).thenReturn("token")
        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(
            syncResponse(sessionManifest().copy(appVerificationEnabled = true))
        )
        whenever(getCachedAccounts()).thenReturn(listOf(mock()))
        whenever(
            consumerRepository.mobileSignUp(
                email = any(),
                phoneNumber = any(),
                country = any(),
                verificationToken = any(),
                appId = any()
            )
        ).thenReturn(signup)
    }

    @Test
    fun `performSignup on verified flows should save account and return success pane on success`() = runTest {
        setupVerifiedFlow()

        val result = handler.performSignup(verifiedFlowState)

        verify(consumerRepository).mobileSignUp(
            email = eq("test@example.com"),
            phoneNumber = eq("+1987654321"),
            country = eq("US"),
            verificationToken = eq("token"),
            appId = eq("applicationId")
        )
        verify(saveAccountToLink).existing(any(), any(), any(), any())
        assertEquals(Pane.SUCCESS, result)
    }

    @Test
    fun `performSignup on verified flows uses consumer session linkBrand when present`() = runTest {
        val signupWithBrand = ConsumerSessionSignup(
            publishableKey = "pk_123",
            consumerSession = consumerSession().copy(linkBrand = LinkBrand.Notlink),
        )
        setupVerifiedFlow(signup = signupWithBrand)

        handler.performSignup(verifiedFlowState)

        verify(saveAccountToLink).existing(
            consumerSessionClientSecret = any(),
            selectedAccounts = any(),
            shouldPollAccountNumbers = any(),
            linkBrand = eq(LinkBrand.Notlink),
        )
    }

    @Test
    fun `performSignup on verified flows falls back to manifest linkBrand when consumer linkBrand is null`() = runTest {
        setupVerifiedFlow()

        handler.performSignup(verifiedFlowState)

        verify(saveAccountToLink).existing(
            consumerSessionClientSecret = any(),
            selectedAccounts = any(),
            shouldPollAccountNumbers = any(),
            linkBrand = eq(LinkBrand.Link),
        )
    }

    @Test
    fun `handleSignupFailure should log error and navigate to Success pane`() = runTest {
        val error = RuntimeException("Test Error")
        val testState = NetworkingLinkSignupState(
            validEmail = "test@instantdebits.com",
            validPhone = "+1234567890",
            isInstantDebits = true,
            payload = Async.Success(validPayload)
        )

        handler.handleSignupFailure(testState, error)

        verify(eventTracker).logError(
            extraMessage = "Error saving account to Link",
            error = error,
            logger = logger,
            pane = Pane.NETWORKING_LINK_SIGNUP_PANE
        )

        navigationManager.assertNavigatedTo(
            destination = Destination.Success,
            pane = Pane.NETWORKING_LINK_SIGNUP_PANE
        )
    }

    @Test
    fun `performSignup should use legacy signup flow when verification is false`() = runTest {
        val testState = NetworkingLinkSignupState(
            validEmail = "legacy@example.com",
            validPhone = "+1987654321",
            isInstantDebits = false,
            payload = Async.Success(validPayload)
        )

        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(
            syncResponse(sessionManifest().copy(appVerificationEnabled = false))
        )
        val cachedAccounts = listOf(mock<CachedPartnerAccount>()) // Ensure this matches what you return
        whenever(getCachedAccounts()).thenReturn(cachedAccounts)

        val expectedPane = Pane.SUCCESS

        val result = handler.performSignup(testState)

        verifyNoInteractions(consumerRepository)
        verify(saveAccountToLink).new(
            email = eq("legacy@example.com"),
            phoneNumber = eq("+1987654321"),
            selectedAccounts = eq(cachedAccounts),
            country = eq("US"),
            shouldPollAccountNumbers = eq(true),
            linkBrand = eq(LinkBrand.Link),
        )
        assertEquals(expectedPane, result)
    }
}
