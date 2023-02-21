package com.stripe.android.financialconnections.features.networkingsavetolinkverification

import com.airbnb.mvrx.test.MavericksTestRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSession
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccount
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.ConfirmVerification
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetCachedConsumerSession
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.MarkLinkVerified
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.domain.StartVerification
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.INSTITUTION_PICKER
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.SUCCESS
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class NetworkingSaveToLinkVerificationViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule()

    private val goNext = mock<GoNext>()
    private val confirmVerification = mock<ConfirmVerification>()
    private val startVerification = mock<StartVerification>()
    private val markLinkVerified = mock<MarkLinkVerified>()
    private val getCachedAccounts = mock<GetCachedAccounts>()
    private val getCachedConsumerSession = mock<GetCachedConsumerSession>()
    private val saveAccountToLink = mock<SaveAccountToLink>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()

    private fun buildViewModel(
        state: NetworkingSaveToLinkVerificationState = NetworkingSaveToLinkVerificationState()
    ) = NetworkingSaveToLinkVerificationViewModel(
        goNext = goNext,
        eventTracker = eventTracker,
        confirmVerification = confirmVerification,
        markLinkVerified = markLinkVerified,
        startVerification = startVerification,
        getCachedAccounts = getCachedAccounts,
        saveAccountToLink = saveAccountToLink,
        getCachedConsumerSession = getCachedConsumerSession,
        logger = Logger.noop(),
        initialState = state
    )

    @Test
    fun `init - starts verification with consumer session secret from cached session`() = runTest {
        val consumerSession = consumerSession()
        whenever(getCachedConsumerSession()).thenReturn(consumerSession)

        val viewModel = buildViewModel()

        val state = viewModel.awaitState()
        verify(startVerification).invoke(consumerSession.clientSecret)
        assertThat(state.payload()!!.consumerSessionClientSecret)
            .isEqualTo(consumerSession.clientSecret)
    }

    @Test
    fun `otpEntered - on valid OTP confirms, saves accounts and navigates to success pane`() =
        runTest {
            val consumerSession = consumerSession()
            val selectedAccount = partnerAccount()
            val linkVerifiedManifest = sessionManifest().copy(nextPane = INSTITUTION_PICKER)
            whenever(getCachedConsumerSession()).thenReturn(consumerSession)
            whenever(markLinkVerified()).thenReturn(linkVerifiedManifest)
            whenever(getCachedAccounts()).thenReturn(listOf(selectedAccount))

            val viewModel = buildViewModel()

            val otpController = viewModel.awaitState().payload()!!.otpElement.controller

            // enters valid OTP
            for (i in 0 until otpController.otpLength) {
                otpController.onValueChanged(i, "1")
            }

            val state = viewModel.awaitState()
            verify(saveAccountToLink).existing(
                eq(state.payload()!!.consumerSessionClientSecret),
                eq(listOf(selectedAccount.id))
            )
            verify(confirmVerification).invoke(any(), eq("111111"))
            verify(goNext).invoke(SUCCESS)
        }
}
