package com.stripe.android.financialconnections.features.networkinglinkverification

import com.airbnb.mvrx.test.MavericksTestRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSession
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccount
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccountList
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.ConfirmVerification
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.LookupAccount
import com.stripe.android.financialconnections.domain.MarkLinkVerified
import com.stripe.android.financialconnections.domain.PollNetworkedAccounts
import com.stripe.android.financialconnections.domain.StartVerification
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.INSTITUTION_PICKER
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.LINK_ACCOUNT_PICKER
import com.stripe.android.financialconnections.model.PartnerAccount
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class NetworkingLinkVerificationViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule()

    private val getManifest = mock<GetManifest>()
    private val goNext = mock<GoNext>()
    private val confirmVerification = mock<ConfirmVerification>()
    private val pollNetworkedAccounts = mock<PollNetworkedAccounts>()
    private val lookupAccount = mock<LookupAccount>()
    private val startVerification = mock<StartVerification>()
    private val markLinkVerified = mock<MarkLinkVerified>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()

    private fun buildViewModel(
        state: NetworkingLinkVerificationState = NetworkingLinkVerificationState()
    ) = NetworkingLinkVerificationViewModel(
        goNext = goNext,
        getManifest = getManifest,
        eventTracker = eventTracker,
        lookupAccount = lookupAccount,
        confirmVerification = confirmVerification,
        markLinkVerified = markLinkVerified,
        pollNetworkedAccounts = pollNetworkedAccounts,
        startVerification = startVerification,
        logger = Logger.noop(),
        initialState = state
    )

    @Test
    fun `init - starts SMS verification with consumer session secret`() = runTest {
        val email = "test@test.com"
        val consumerSession = consumerSession()
        getManifestReturnsManifestWithEmail(email)
        lookupAccountReturns(email, consumerSession)

        val viewModel = buildViewModel()

        val state = viewModel.awaitState()
        verify(startVerification).sms(consumerSession.clientSecret)
        assertThat(state.payload()!!.consumerSessionClientSecret)
            .isEqualTo(consumerSession.clientSecret)
    }

    @Test
    fun `otpEntered - on valid OTP and no accounts confirms and navigates to manifest next pane`() =
        runTest {
            val email = "test@test.com"
            val consumerSession = consumerSession()
            val linkVerifiedManifest = sessionManifest().copy(nextPane = INSTITUTION_PICKER)
            getManifestReturnsManifestWithEmail(email)
            // polling returns no networked accounts
            pollNetworkedAccountsReturns(emptyList())
            lookupAccountReturns(email, consumerSession)
            markLinkVerifiedReturns(linkVerifiedManifest)

            val viewModel = buildViewModel()

            val otpController = viewModel.awaitState().payload()!!.otpElement.controller

            // enters valid OTP
            for (i in 0 until otpController.otpLength) {
                otpController.onValueChanged(i, "1")
            }

            verify(confirmVerification).sms(any(), eq("111111"))
            verify(goNext).invoke(linkVerifiedManifest.nextPane)
        }

    @Test
    fun `otpEntered - valid OTP and accounts confirms and navigates to LINK_ACCOUNT_PICKER`() =
        runTest {
            val email = "test@test.com"
            val consumerSession = consumerSession()
            val linkVerifiedManifest = sessionManifest().copy(nextPane = INSTITUTION_PICKER)
            getManifestReturnsManifestWithEmail(email)
            // polling returns some networked accounts
            pollNetworkedAccountsReturns(listOf(partnerAccount()))
            lookupAccountReturns(email, consumerSession)
            markLinkVerifiedReturns(linkVerifiedManifest)

            val viewModel = buildViewModel()

            val otpController = viewModel.awaitState().payload()!!.otpElement.controller

            // enters valid OTP
            for (i in 0 until otpController.otpLength) {
                otpController.onValueChanged(i, "1")
            }

            verify(confirmVerification).sms(any(), eq("111111"))
            verify(goNext).invoke(LINK_ACCOUNT_PICKER)
        }

    private suspend fun getManifestReturnsManifestWithEmail(
        email: String
    ) {
        whenever(getManifest()).thenReturn(
            sessionManifest().copy(
                accountholderCustomerEmailAddress = email
            )
        )
    }

    private suspend fun markLinkVerifiedReturns(
        manifest: FinancialConnectionsSessionManifest
    ) {
        whenever(markLinkVerified()).thenReturn(manifest)
    }

    private suspend fun pollNetworkedAccountsReturns(
        list: List<PartnerAccount>
    ) {
        whenever(pollNetworkedAccounts(any()))
            .thenReturn(partnerAccountList().copy(data = list))
    }

    private suspend fun lookupAccountReturns(
        email: String,
        consumerSession: ConsumerSession
    ) {
        whenever(lookupAccount(eq(email))).thenReturn(
            ConsumerSessionLookup(
                consumerSession = consumerSession,
                exists = true,
                errorMessage = null
            )
        )
    }
}
