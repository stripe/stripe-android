package com.stripe.android.financialconnections.features.networkinglinksignup

import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSessionSignup
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.domain.AttachConsumerToLinkAccountSession
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.HandleError
import com.stripe.android.financialconnections.domain.RequestIntegrityToken
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals

class LinkSignupHandlerForInstantDebitsTest {

    private lateinit var handler: LinkSignupHandlerForInstantDebits
    private val consumerRepository = mock<FinancialConnectionsConsumerSessionRepository>()
    private val getOrFetchSync = mock<GetOrFetchSync>()
    private val attachConsumerToLinkAccountSession = mock<AttachConsumerToLinkAccountSession>()
    private val requestIntegrityToken = mock<RequestIntegrityToken>()
    private val navigationManager = mock<NavigationManager>()
    private val handleError = mock<HandleError>()

    @Before
    fun setUp() {
        handler = LinkSignupHandlerForInstantDebits(
            consumerRepository,
            attachConsumerToLinkAccountSession,
            requestIntegrityToken,
            getOrFetchSync,
            navigationManager,
            "applicationId",
            handleError
        )
    }

    private val validPayload = Payload(
        merchantName = "Mock Merchant",
        emailController = mock(),
        prefilledEmail = "test@stripe.com",
        sessionId = "fcsess_1234",
        appVerificationEnabled = false,
        phoneController = mock {
            whenever(it.getCountryCode()).thenReturn("US")
        },
        isInstantDebits = true,
        content = mock()
    )

    @Test
    fun `performSignup should navigate to next pane on success`() = runTest {
        val expectedToken = "token"
        val expectedPane = Pane.INSTITUTION_PICKER
        val testState = NetworkingLinkSignupState(
            validEmail = "test@example.com",
            validPhone = "+123456789",
            isInstantDebits = true,
            payload = Async.Success(validPayload)
        )

        whenever(requestIntegrityToken(anyOrNull(), anyOrNull())).thenReturn(expectedToken)
        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(
            syncResponse(
                sessionManifest().copy(
                    nextPane = expectedPane,
                    appVerificationEnabled = true
                )
            )
        )
        whenever(
            consumerRepository.mobileSignUp(
                email = any(),
                phoneNumber = any(),
                country = any(),
                verificationToken = any(),
                appId = any()
            )
        ).thenReturn(consumerSessionSignup())

        val result = handler.performSignup(testState)

        verify(attachConsumerToLinkAccountSession).invoke(any())
        assertEquals(expectedPane, result)
    }

    @Test
    fun `handleSignupFailure should call handleError with correct parameters`() = runTest {
        val testState = NetworkingLinkSignupState(
            validEmail = "test@instantdebits.com",
            validPhone = "+1234567890",
            isInstantDebits = true,
            payload = Async.Success(validPayload)
        )
        val error = RuntimeException("Test Error")

        handler.handleSignupFailure(testState, error)

        verify(handleError).invoke(
            extraMessage = "Error creating a Link account",
            error = error,
            pane = Pane.LINK_LOGIN,
            displayErrorScreen = true
        )
    }

    @Test
    fun `performSignup should proceed without verification when appVerificationEnabled is false`() = runTest {
        val testState = NetworkingLinkSignupState(
            validEmail = "test@instantdebits.com",
            validPhone = "+1234567890",
            isInstantDebits = true,
            payload = Async.Success(validPayload)
        )

        whenever(getOrFetchSync(anyOrNull(), anyOrNull())).thenReturn(
            syncResponse(
                sessionManifest().copy(
                    appVerificationEnabled = false,
                    nextPane = Pane.INSTITUTION_PICKER
                )
            )
        )

        whenever(consumerRepository.signUp(any(), any(), any()))
            .thenReturn(consumerSessionSignup())

        val expectedNextPane = Pane.INSTITUTION_PICKER
        val result = handler.performSignup(testState)

        verify(consumerRepository).signUp(
            email = eq("test@instantdebits.com"),
            phoneNumber = eq("+1234567890"),
            country = eq("US")
        )

        verify(attachConsumerToLinkAccountSession).invoke(any())

        assertEquals(expectedNextPane, result)
    }
}
