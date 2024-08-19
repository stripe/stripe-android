package com.stripe.android.financialconnections.features.networkinglinkverification

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.AttachConsumerToLinkAccountSession
import com.stripe.android.financialconnections.domain.ConfirmVerification
import com.stripe.android.financialconnections.domain.GetCachedConsumerSession
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.GetOrFetchSync.RefetchCondition
import com.stripe.android.financialconnections.domain.MarkLinkVerified
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.StartVerification
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.INSTITUTION_PICKER
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.NETWORKING_LINK_VERIFICATION
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.repository.CachedConsumerSession
import com.stripe.android.financialconnections.utils.TestNavigationManager
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSession.VerificationSession.SessionState
import com.stripe.android.model.ConsumerSession.VerificationSession.SessionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class NetworkingLinkVerificationViewModelTest {

    @get:Rule
    val testRule = CoroutineTestRule()

    private val getOrFetchSync = mock<GetOrFetchSync>()
    private val navigationManager = TestNavigationManager()
    private val confirmVerification = mock<ConfirmVerification>()
    private val markLinkVerified = mock<MarkLinkVerified>()
    private val analyticsTracker = TestFinancialConnectionsAnalyticsTracker()
    private val startVerification = mock<StartVerification>()
    private val nativeAuthFlowCoordinator = NativeAuthFlowCoordinator()
    private val getCachedConsumerSession = mock<GetCachedConsumerSession>()
    private val attachConsumerToLinkAccountSession = mock<AttachConsumerToLinkAccountSession>()

    private fun buildViewModel(
        state: NetworkingLinkVerificationState = NetworkingLinkVerificationState(),
        isLinkWithStripe: Boolean = false,
    ) = NetworkingLinkVerificationViewModel(
        navigationManager = navigationManager,
        getOrFetchSync = getOrFetchSync,
        confirmVerification = confirmVerification,
        markLinkVerified = markLinkVerified,
        analyticsTracker = analyticsTracker,
        startVerification = startVerification,
        logger = Logger.noop(),
        initialState = state,
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        getCachedConsumerSession = getCachedConsumerSession,
        isLinkWithStripe = { isLinkWithStripe },
        attachConsumerToLinkAccountSession = attachConsumerToLinkAccountSession,
    )

    @Test
    fun `init - renders initial state with cached consumer session`() = runTest {
        whenever(getOrFetchSync()).thenReturn(
            syncResponse(sessionManifest())
        )

        whenever(getCachedConsumerSession()).thenReturn(
            CachedConsumerSession(
                emailAddress = "email@email.com",
                phoneNumber = "(•••) •••-••42",
                clientSecret = "secret",
                publishableKey = "pk_123",
                isVerified = false,
            )
        )

        whenever(startVerification.sms("secret")).thenReturn(
            mockConsumerSessionWithPendingVerification()
        )

        val viewModel = buildViewModel()
        val payload = viewModel.stateFlow.value.payload()

        assertThat(payload?.email).isEqualTo("email@email.com")
        assertThat(payload?.phoneNumber).isEqualTo("(•••) •••-••42")
        assertThat(payload?.consumerSessionClientSecret).isEqualTo("secret")
    }

    @Test
    fun `init - fails gracefully if unable to find cached consumer session`() = runTest {
        whenever(getOrFetchSync()).thenReturn(
            syncResponse(sessionManifest())
        )

        whenever(getCachedConsumerSession()).thenReturn(null)

        val viewModel = buildViewModel()
        val payload = viewModel.stateFlow.value.payload

        assertThat(payload).isInstanceOf(Async.Fail::class.java)
    }

    @Test
    fun `otpEntered - valid OTP and confirms navigates to LINK_ACCOUNT_PICKER`() =
        runTest {
            val email = "test@test.com"
            val linkVerifiedManifest = sessionManifest().copy(nextPane = INSTITUTION_PICKER)

            whenever(getOrFetchSync()).thenReturn(
                syncResponse(sessionManifest().copy(accountholderCustomerEmailAddress = email))
            )

            whenever(getCachedConsumerSession()).thenReturn(
                CachedConsumerSession(
                    emailAddress = "email@email.com",
                    phoneNumber = "(•••) •••-••42",
                    clientSecret = "secret",
                    publishableKey = "pk_123",
                    isVerified = false,
                )
            )

            whenever(startVerification.sms("secret")).thenReturn(
                mockConsumerSessionWithPendingVerification()
            )

            // polling returns some networked accounts
            whenever(markLinkVerified()).thenReturn((linkVerifiedManifest))

            val viewModel = buildViewModel()
            val otpController = viewModel.stateFlow.value.payload()!!.otpElement.controller

            // enters valid OTP
            for (i in 0 until otpController.otpLength) {
                otpController.onValueChanged(i, "1")
            }

            verify(confirmVerification).sms(any(), eq("111111"))
            navigationManager.assertNavigatedTo(
                destination = Destination.LinkAccountPicker,
                pane = NETWORKING_LINK_VERIFICATION
            )
        }

    @Test
    fun `otpEntered - save to link fails with no initial institution navigates to INSTITUTI`() =
        runTest {
            val email = "test@test.com"

            whenever(getOrFetchSync()).thenReturn(
                syncResponse(
                    sessionManifest().copy(
                        accountholderCustomerEmailAddress = email,
                        initialInstitution = null
                    )
                )
            )

            whenever(getCachedConsumerSession()).thenReturn(
                CachedConsumerSession(
                    emailAddress = "email@email.com",
                    phoneNumber = "(•••) •••-••42",
                    clientSecret = "secret",
                    publishableKey = "pk_123",
                    isVerified = false,
                )
            )

            whenever(startVerification.sms("secret")).thenReturn(
                mockConsumerSessionWithPendingVerification()
            )

            // polling returns some networked accounts
            whenever(markLinkVerified()).thenAnswer {
                throw LocalStripeException(
                    displayMessage = "error marking link as verified",
                    analyticsValue = null
                )
            }

            val viewModel = buildViewModel()
            val otpController = viewModel.stateFlow.value.payload()!!.otpElement.controller

            // enters valid OTP
            for (i in 0 until otpController.otpLength) {
                otpController.onValueChanged(i, "1")
            }

            verify(confirmVerification).sms(any(), eq("111111"))
            navigationManager.assertNavigatedTo(
                destination = Destination.InstitutionPicker,
                pane = NETWORKING_LINK_VERIFICATION
            )
        }

    @Test
    fun `otpEntered - attaches consumer to LAS and navigates to account picker in Instant Debits`() = runTest {
        val email = "email@email.com"

        whenever(getOrFetchSync(any())).thenReturn(
            syncResponse(sessionManifest().copy(accountholderCustomerEmailAddress = email))
        )

        whenever(getCachedConsumerSession()).thenReturn(
            CachedConsumerSession(
                emailAddress = "email@email.com",
                phoneNumber = "(•••) •••-••42",
                clientSecret = "secret",
                publishableKey = "pk_123",
                isVerified = false,
            )
        )

        whenever(startVerification.sms("secret")).thenReturn(
            mockConsumerSessionWithPendingVerification()
        )

        whenever(attachConsumerToLinkAccountSession.invoke(any())).thenReturn(Unit)

        val viewModel = buildViewModel(isLinkWithStripe = true)
        val otpController = viewModel.stateFlow.value.payload()!!.otpElement.controller
        val otpCode = "111111"

        for (index in otpCode.indices) {
            otpController.onValueChanged(index, otpCode[index].toString())
        }

        verify(attachConsumerToLinkAccountSession).invoke("secret")
        verify(getOrFetchSync).invoke(RefetchCondition.Always)
        verify(markLinkVerified, never()).invoke()

        navigationManager.assertNavigatedTo(
            destination = Destination.LinkAccountPicker,
            pane = NETWORKING_LINK_VERIFICATION,
        )
    }

    private fun mockConsumerSessionWithPendingVerification(): ConsumerSession {
        return ConsumerSession(
            clientSecret = "secret",
            emailAddress = "email@email.com",
            redactedPhoneNumber = "(•••) •••-••42",
            redactedFormattedPhoneNumber = "(•••) •••-••42",
            verificationSessions = listOf(
                ConsumerSession.VerificationSession(
                    type = SessionType.Sms,
                    state = SessionState.Started,
                ),
            ),
        )
    }
}
