package com.stripe.android.financialconnections.features.linkstepupverification

import com.airbnb.mvrx.Loading
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
import com.stripe.android.financialconnections.domain.LookupConsumerAndStartVerification
import com.stripe.android.financialconnections.domain.MarkLinkStepUpVerified
import com.stripe.android.financialconnections.domain.SelectNetworkedAccount
import com.stripe.android.financialconnections.domain.UpdateCachedAccounts
import com.stripe.android.financialconnections.domain.UpdateLocalManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.utils.TestNavigationManager
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.VerificationType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class LinkStepUpVerificationViewModelTest {
    @get:Rule
    val mavericksTestRule = MavericksTestRule()

    private val getManifest = mock<GetManifest>()
    private val navigationManager = TestNavigationManager()
    private val confirmVerification = mock<ConfirmVerification>()
    private val getCachedAccounts = mock<GetCachedAccounts>()
    private val lookupConsumerAndStartVerification = mock<LookupConsumerAndStartVerification>()
    private val selectNetworkedAccount = mock<SelectNetworkedAccount>()
    private val markLinkVerified = mock<MarkLinkStepUpVerified>()
    private val updateLocalManifest = mock<UpdateLocalManifest>()
    private val updateCachedAccounts = mock<UpdateCachedAccounts>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()

    private fun buildViewModel(
        state: LinkStepUpVerificationState = LinkStepUpVerificationState()
    ) = LinkStepUpVerificationViewModel(
        getManifest = getManifest,
        navigationManager = navigationManager,
        eventTracker = eventTracker,
        confirmVerification = confirmVerification,
        markLinkStepUpVerified = markLinkVerified,
        getCachedAccounts = getCachedAccounts,
        selectNetworkedAccount = selectNetworkedAccount,
        updateCachedAccounts = updateCachedAccounts,
        updateLocalManifest = updateLocalManifest,
        lookupConsumerAndStartVerification = lookupConsumerAndStartVerification,
        logger = Logger.noop(),
        initialState = state
    )

    @Test
    fun `init - starts EMAIL verification with consumer session secret`() = runTest {
        val email = "test@test.com"
        val consumerSession = ApiKeyFixtures.consumerSession()
        getManifestReturnsManifestWithEmail(email)

        val onStartVerificationCaptor = argumentCaptor<suspend () -> Unit>()
        val onVerificationStartedCaptor = argumentCaptor<suspend (ConsumerSession) -> Unit>()

        val viewModel = buildViewModel()

        verify(lookupConsumerAndStartVerification).invoke(
            email = eq(email),
            businessName = anyOrNull(),
            verificationType = eq(VerificationType.EMAIL),
            onConsumerNotFound = any(),
            onLookupError = any(),
            onStartVerification = onStartVerificationCaptor.capture(),
            onVerificationStarted = onVerificationStartedCaptor.capture(),
            onStartVerificationError = any()
        )

        onStartVerificationCaptor.firstValue()
        onVerificationStartedCaptor.firstValue(consumerSession)

        val state = viewModel.awaitState()

        assertThat(state.payload()!!.consumerSessionClientSecret)
            .isEqualTo(consumerSession.clientSecret)
    }

    @Test
    fun `init - ConsumerNotFound sends analytics and navigates to institution picker`() = runTest {
        val email = "test@test.com"
        val onConsumerNotFoundCaptor = argumentCaptor<suspend () -> Unit>()

        whenever(getManifest()).thenReturn(
            sessionManifest().copy(accountholderCustomerEmailAddress = email)
        )

        val viewModel = buildViewModel()

        assertThat(viewModel.awaitState().payload).isInstanceOf(Loading::class.java)

        verify(lookupConsumerAndStartVerification).invoke(
            email = eq(email),
            businessName = anyOrNull(),
            verificationType = eq(VerificationType.EMAIL),
            onConsumerNotFound = onConsumerNotFoundCaptor.capture(),
            onLookupError = any(),
            onStartVerification = any(),
            onVerificationStarted = any(),
            onStartVerificationError = any()
        )

        onConsumerNotFoundCaptor.firstValue()

        assertThat(viewModel.awaitState().payload).isInstanceOf(Loading::class.java)
        navigationManager.assertNavigatedTo(
            destination = Destination.InstitutionPicker,
            pane = Pane.LINK_STEP_UP_VERIFICATION
        )
        eventTracker.assertContainsEvent(
            "linked_accounts.networking.verification.step_up.error",
            mapOf(
                "pane" to "networking_link_step_up_verification",
                "error" to "ConsumerNotFoundError"
            )
        )
    }

    @Test
    fun `otpEntered - on valid OTP confirms, verifies, selects account and navigates to success`() =
        runTest {
            val email = "test@test.com"
            val consumerSession = ApiKeyFixtures.consumerSession()
            val onStartVerificationCaptor = argumentCaptor<suspend () -> Unit>()
            val onVerificationStartedCaptor = argumentCaptor<suspend (ConsumerSession) -> Unit>()
            val linkVerifiedManifest = sessionManifest().copy(nextPane = Pane.INSTITUTION_PICKER)
            val selectedAccount = partnerAccount()

            // verify succeeds
            whenever(getManifest()).thenReturn(
                sessionManifest().copy(accountholderCustomerEmailAddress = email)
            )
            // cached accounts are available.
            whenever(getCachedAccounts()).thenReturn(listOf(selectedAccount))
            // link succeeds
            markLinkVerifiedReturns(linkVerifiedManifest)

            val viewModel = buildViewModel()

            verify(lookupConsumerAndStartVerification).invoke(
                email = eq(email),
                businessName = anyOrNull(),
                verificationType = eq(VerificationType.EMAIL),
                onConsumerNotFound = any(),
                onLookupError = any(),
                onStartVerification = onStartVerificationCaptor.capture(),
                onVerificationStarted = onVerificationStartedCaptor.capture(),
                onStartVerificationError = any()
            )

            onStartVerificationCaptor.firstValue()
            onVerificationStartedCaptor.firstValue(consumerSession)

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
            navigationManager.assertNavigatedTo(
                Destination.Success,
                pane = Pane.LINK_STEP_UP_VERIFICATION
            )
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
}
