package com.stripe.android.financialconnections.features.networkinglinkverification

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.financialconnections.ApiKeyFixtures.cachedConsumerSession
import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSession
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.AttachConsumerToLinkAccountSession
import com.stripe.android.financialconnections.domain.ConfirmVerification
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.GetOrFetchSync.RefetchCondition
import com.stripe.android.financialconnections.domain.HandleError
import com.stripe.android.financialconnections.domain.MarkLinkVerified
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.StartVerification
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.INSTITUTION_PICKER
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.NETWORKING_LINK_VERIFICATION
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.repository.CachedConsumerSession
import com.stripe.android.financialconnections.utils.TestNavigationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
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
    private val startVerification = mock<StartVerification>()
    private val markLinkVerified = mock<MarkLinkVerified>()
    private val analyticsTracker = TestFinancialConnectionsAnalyticsTracker()
    private val nativeAuthFlowCoordinator = NativeAuthFlowCoordinator()
    private val attachConsumerToLinkAccountSession = mock<AttachConsumerToLinkAccountSession>()
    private val handleError = mock<HandleError>()

    private fun buildViewModel(
        state: NetworkingLinkVerificationState = NetworkingLinkVerificationState(),
        consumerSession: CachedConsumerSession? = cachedConsumerSession(),
        isLinkWithStripe: Boolean = false,
    ) = NetworkingLinkVerificationViewModel(
        navigationManager = navigationManager,
        getOrFetchSync = getOrFetchSync,
        startVerification = startVerification,
        confirmVerification = confirmVerification,
        markLinkVerified = markLinkVerified,
        analyticsTracker = analyticsTracker,
        logger = Logger.noop(),
        initialState = state,
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
        consumerSessionProvider = { consumerSession },
        isLinkWithStripe = { isLinkWithStripe },
        attachConsumerToLinkAccountSession = attachConsumerToLinkAccountSession,
        handleError = handleError,
    )

    @Test
    fun `init - starts SMS verification with consumer session secret`() = runTest {
        val email = "test@test.com"
        val consumerSession = consumerSession()
        whenever(getOrFetchSync()).thenReturn(
            syncResponse(sessionManifest().copy(accountholderCustomerEmailAddress = email))
        )
        whenever(startVerification.sms(consumerSession.clientSecret)).doReturn(consumerSession)

        val viewModel = buildViewModel()

        val state = viewModel.stateFlow.value
        assertThat(state.payload()!!.consumerSessionClientSecret)
            .isEqualTo(consumerSession.clientSecret)
    }

    @Test
    fun `otpEntered - valid OTP and confirms navigates to LINK_ACCOUNT_PICKER`() =
        runTest {
            val email = "test@test.com"
            val consumerSession = consumerSession()
            val linkVerifiedManifest = sessionManifest().copy(nextPane = INSTITUTION_PICKER)
            whenever(getOrFetchSync()).thenReturn(
                syncResponse(sessionManifest().copy(accountholderCustomerEmailAddress = email))
            )

            whenever(startVerification.sms(any())).doReturn(consumerSession)

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
            val consumerSession = consumerSession()

            whenever(getOrFetchSync()).thenReturn(
                syncResponse(
                    sessionManifest().copy(
                        accountholderCustomerEmailAddress = email,
                        initialInstitution = null
                    )
                )
            )

            whenever(startVerification.sms(any())).doReturn(consumerSession)

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
        val consumerSession = consumerSession()

        whenever(getOrFetchSync(any(), anyOrNull())).thenReturn(
            syncResponse(sessionManifest().copy(accountholderCustomerEmailAddress = consumerSession.emailAddress))
        )
        whenever(startVerification.sms(any())).doReturn(consumerSession)
        whenever(attachConsumerToLinkAccountSession.invoke(any())).thenReturn(Unit)

        val viewModel = buildViewModel(isLinkWithStripe = true)

        val otpController = viewModel.stateFlow.value.payload()!!.otpElement.controller

        val otpCode = "111111"

        for (index in otpCode.indices) {
            otpController.onValueChanged(index, otpCode[index].toString())
        }

        verify(attachConsumerToLinkAccountSession).invoke(consumerSession.clientSecret)
        verify(getOrFetchSync).invoke(RefetchCondition.Always)
        verify(markLinkVerified, never()).invoke()

        navigationManager.assertNavigatedTo(
            destination = Destination.LinkAccountPicker,
            pane = NETWORKING_LINK_VERIFICATION,
        )
    }

    @Test
    fun `otpEntered - shows terminal error if failing to attach consumer to LAS in Instant Debits`() = runTest {
        val consumerSession = consumerSession()

        whenever(getOrFetchSync(any(), anyOrNull())).thenReturn(
            syncResponse(sessionManifest().copy(accountholderCustomerEmailAddress = consumerSession.emailAddress))
        )

        whenever(startVerification.sms(any())).doReturn(consumerSession)

        whenever(attachConsumerToLinkAccountSession.invoke(any())).then {
            throw APIConnectionException()
        }

        val viewModel = buildViewModel(isLinkWithStripe = true)

        val otpController = viewModel.stateFlow.value.payload()!!.otpElement.controller

        val otpCode = "111111"

        for (index in otpCode.indices) {
            otpController.onValueChanged(index, otpCode[index].toString())
        }

        verify(attachConsumerToLinkAccountSession).invoke(consumerSession.clientSecret)
        verify(handleError).invoke(
            extraMessage = any(),
            error = any(),
            pane = eq(NETWORKING_LINK_VERIFICATION),
            displayErrorScreen = eq(true),
        )
    }
}
