package com.stripe.android.financialconnections.features.networkingsavetolinkverification

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSession
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.CompleteVerification
import com.stripe.android.financialconnections.domain.ConfirmVerification
import com.stripe.android.financialconnections.domain.GetCachedConsumerSession
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.StartVerification
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.utils.TestNavigationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class NetworkingSaveToLinkVerificationViewModelTest {

    @get:Rule
    val testRule = CoroutineTestRule()

    private val navigationManager = TestNavigationManager()
    private val confirmVerification = mock<ConfirmVerification>()
    private val startVerification = mock<StartVerification>()
    private val getCachedConsumerSession = mock<GetCachedConsumerSession>()
    private val getOrFetchSync = mock<GetOrFetchSync>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val nativeAuthFlowCoordinator = NativeAuthFlowCoordinator()
    private val completeVerification = mock<CompleteVerification>()

    private fun buildViewModel(
        state: NetworkingSaveToLinkVerificationState = NetworkingSaveToLinkVerificationState()
    ) = NetworkingSaveToLinkVerificationViewModel(
        navigationManager = navigationManager,
        eventTracker = eventTracker,
        confirmVerification = confirmVerification,
        startVerification = startVerification,
        completeVerification = completeVerification,
        getCachedConsumerSession = getCachedConsumerSession,
        getOrFetchSync = getOrFetchSync,
        logger = Logger.noop(),
        initialState = state,
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
    )

    @Test
    fun `init - starts verification with consumer session secret from cached session`() = runTest {
        val consumerSession = consumerSession()
        whenever(getOrFetchSync()).thenReturn(syncResponse(sessionManifest()))
        whenever(getCachedConsumerSession()).thenReturn(consumerSession)

        val viewModel = buildViewModel()

        val state = viewModel.stateFlow.value
        verify(startVerification).sms(consumerSession.clientSecret)
        assertThat(state.payload()!!.consumerSessionClientSecret)
            .isEqualTo(consumerSession.clientSecret)
    }

    @Test
    fun `otpEntered - on valid OTP confirms, saves accounts and navigates to success pane`() = runTest {
        val consumerSession = consumerSession()
        whenever(getCachedConsumerSession()).thenReturn(consumerSession)
        whenever(getOrFetchSync()).thenReturn(syncResponse(sessionManifest()))

        val viewModel = buildViewModel()

        val otpController = viewModel.stateFlow.value.payload()!!.otpElement.controller

        // enters valid OTP
        for (i in 0 until otpController.otpLength) {
            otpController.onValueChanged(i, "1")
        }

        verify(confirmVerification).sms(
            consumerSessionClientSecret = consumerSession.clientSecret,
            verificationCode = "111111"
        )
        verify(completeVerification).invoke(
            consumerSessionClientSecret = consumerSession.clientSecret,
            pane = Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION
        )
    }

    @Test
    fun `onSkipClick - navigates to success without networking accounts`() = runTest {
        val consumerSession = consumerSession()
        whenever(getCachedConsumerSession()).thenReturn(consumerSession)

        val viewModel = buildViewModel()

        viewModel.onSkipClick()

        verifyNoInteractions(confirmVerification)
        verifyNoInteractions(completeVerification)
        navigationManager.assertNavigatedTo(
            destination = Destination.Success,
            pane = Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION
        )
    }
}
