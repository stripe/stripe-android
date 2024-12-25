package com.stripe.android.financialconnections.features.networkinglinksignup

import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSessionSignup
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.domain.AttachConsumerToLinkAccountSession
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.HandleError
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.financialconnections.utils.TestIntegrityRequestManager
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
    private val integrityRequestManager = TestIntegrityRequestManager()
    private val navigationManager = mock<NavigationManager>()
    private val handleError = mock<HandleError>()

    @Before
    fun setUp() {
        handler = LinkSignupHandlerForInstantDebits(
            consumerRepository,
            attachConsumerToLinkAccountSession,
            integrityRequestManager,
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
        val testState = NetworkingLinkSignupState(
            validEmail = "test@example.com",
            validPhone = "+123456789",
            isInstantDebits = true,
            payload = Async.Success(validPayload)
        )

        val expectedPane = Pane.INSTITUTION_PICKER
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
        val error = RuntimeException("Test Error")
        handler.handleSignupFailure(error)

        verify(handleError).invoke(
            extraMessage = "Error creating a Link account",
            error = error,
            pane = Pane.LINK_LOGIN,
            displayErrorScreen = true
        )
    }
}
