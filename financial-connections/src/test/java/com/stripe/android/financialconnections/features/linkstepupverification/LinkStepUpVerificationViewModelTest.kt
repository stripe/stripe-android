package com.stripe.android.financialconnections.features.linkstepupverification

import com.airbnb.mvrx.test.MavericksTestRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.ApiKeyFixtures.partnerAccount
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.ConfirmVerification
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.LookupAccount
import com.stripe.android.financialconnections.domain.MarkLinkStepUpVerified
import com.stripe.android.financialconnections.domain.SelectNetworkedAccount
import com.stripe.android.financialconnections.domain.StartVerification
import com.stripe.android.financialconnections.domain.UpdateCachedAccounts
import com.stripe.android.financialconnections.domain.UpdateLocalManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
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

class LinkStepUpVerificationViewModelTest {
    @get:Rule
    val mavericksTestRule = MavericksTestRule()

    private val getManifest = mock<GetManifest>()
    private val goNext = mock<GoNext>()
    private val confirmVerification = mock<ConfirmVerification>()
    private val getCachedAccounts = mock<GetCachedAccounts>()
    private val lookupAccount = mock<LookupAccount>()
    private val startVerification = mock<StartVerification>()
    private val selectNetworkedAccount = mock<SelectNetworkedAccount>()
    private val markLinkVerified = mock<MarkLinkStepUpVerified>()
    private val updateLocalManifest = mock<UpdateLocalManifest>()
    private val updateCachedAccounts = mock<UpdateCachedAccounts>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()

    private fun buildViewModel(
        state: LinkStepUpVerificationState = LinkStepUpVerificationState()
    ) = LinkStepUpVerificationViewModel(
        goNext = goNext,
        getManifest = getManifest,
        eventTracker = eventTracker,
        lookupAccount = lookupAccount,
        confirmVerification = confirmVerification,
        markLinkStepUpVerified = markLinkVerified,
        startVerification = startVerification,
        getCachedAccounts = getCachedAccounts,
        selectNetworkedAccount = selectNetworkedAccount,
        updateCachedAccounts = updateCachedAccounts,
        updateLocalManifest = updateLocalManifest,
        logger = Logger.noop(),
        initialState = state
    )

    @Test
    fun `init - starts EMAIL verification with consumer session secret`() = runTest {
        val email = "test@test.com"
        val consumerSession = ApiKeyFixtures.consumerSession()
        getManifestReturnsManifestWithEmail(email)
        lookupAccountReturns(email, consumerSession)

        val viewModel = buildViewModel()

        val state = viewModel.awaitState()
        verify(startVerification).email(consumerSession.clientSecret)
        assertThat(state.payload()!!.consumerSessionClientSecret)
            .isEqualTo(consumerSession.clientSecret)
    }

    @Test
    fun `otpEntered - on valid OTP confirms, verifies, selects account and navigates to success`() =
        runTest {
            val email = "test@test.com"
            val consumerSession = ApiKeyFixtures.consumerSession()
            val linkVerifiedManifest = sessionManifest().copy(nextPane = Pane.INSTITUTION_PICKER)
            val selectedAccount = partnerAccount()
            getManifestReturnsManifestWithEmail(email)
            whenever(getCachedAccounts()).thenReturn(listOf(selectedAccount))
            lookupAccountReturns(email, consumerSession)
            markLinkVerifiedReturns(linkVerifiedManifest)

            val viewModel = buildViewModel()

            val otpController = viewModel.awaitState().payload()!!.otpElement.controller

            // enters valid OTP
            for (i in 0 until otpController.otpLength) {
                otpController.onValueChanged(i, "1")
            }

            verify(confirmVerification).email(any(), eq("111111"))
            verify(markLinkVerified).invoke()
            verify(selectNetworkedAccount).invoke(
                consumerSession.clientSecret,
                selectedAccount.id
            )
            verify(goNext).invoke(Pane.SUCCESS)
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
