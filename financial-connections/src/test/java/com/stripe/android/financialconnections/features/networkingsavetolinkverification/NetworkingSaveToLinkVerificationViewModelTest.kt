package com.stripe.android.financialconnections.features.networkingsavetolinkverification

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.ApiKeyFixtures.cachedConsumerSession
import com.stripe.android.financialconnections.ApiKeyFixtures.cachedPartnerAccount
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.CoroutineTestRule
import com.stripe.android.financialconnections.TestFinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.domain.ConfirmVerification
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.MarkLinkVerified
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.domain.StartVerification
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.INSTITUTION_PICKER
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.repository.AttachedPaymentAccountRepository
import com.stripe.android.financialconnections.utils.TestNavigationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
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
    private val markLinkVerified = mock<MarkLinkVerified>()
    private val getCachedAccounts = mock<GetCachedAccounts>()
    private val getOrFetchSync = mock<GetOrFetchSync>()
    private val attachedPaymentAccountRepository = mock<AttachedPaymentAccountRepository>()
    private val saveAccountToLink = mock<SaveAccountToLink>()
    private val eventTracker = TestFinancialConnectionsAnalyticsTracker()
    private val nativeAuthFlowCoordinator = NativeAuthFlowCoordinator()
    private val cachedConsumerSession = cachedConsumerSession()

    private fun buildViewModel(
        state: NetworkingSaveToLinkVerificationState = NetworkingSaveToLinkVerificationState()
    ) = NetworkingSaveToLinkVerificationViewModel(
        navigationManager = navigationManager,
        eventTracker = eventTracker,
        confirmVerification = confirmVerification,
        markLinkVerified = markLinkVerified,
        startVerification = startVerification,
        getCachedAccounts = getCachedAccounts,
        saveAccountToLink = saveAccountToLink,
        consumerSessionProvider = { cachedConsumerSession },
        getOrFetchSync = getOrFetchSync,
        logger = Logger.noop(),
        initialState = state,
        attachedPaymentAccountRepository = attachedPaymentAccountRepository,
        nativeAuthFlowCoordinator = nativeAuthFlowCoordinator,
    )

    @Test
    fun `init - starts verification with consumer session secret from cached session`() = runTest {
        whenever(getOrFetchSync()).thenReturn(syncResponse(sessionManifest()))

        val viewModel = buildViewModel()

        val state = viewModel.stateFlow.value
        verify(startVerification).sms(cachedConsumerSession.clientSecret)
        assertThat(state.payload()!!.consumerSessionClientSecret)
            .isEqualTo(cachedConsumerSession.clientSecret)
    }

    @Test
    fun `otpEntered - on valid OTP confirms, saves accounts and navigates to success pane`() =
        runTest {
            val selectedAccount = cachedPartnerAccount()
            val linkVerifiedManifest = sessionManifest().copy(nextPane = INSTITUTION_PICKER)
            whenever(getOrFetchSync()).thenReturn(syncResponse(sessionManifest()))
            whenever(markLinkVerified()).thenReturn(linkVerifiedManifest)
            whenever(getCachedAccounts()).thenReturn(listOf(selectedAccount))

            val viewModel = buildViewModel()

            val otpController = viewModel.stateFlow.value.payload()!!.otpElement.controller

            // enters valid OTP
            for (i in 0 until otpController.otpLength) {
                otpController.onValueChanged(i, "1")
            }

            val state = viewModel.stateFlow.value
            verify(saveAccountToLink).existing(
                eq(state.payload()!!.consumerSessionClientSecret),
                eq(listOf(selectedAccount)),
                eq(true),
            )
            verify(confirmVerification).sms(
                consumerSessionClientSecret = cachedConsumerSession.clientSecret,
                verificationCode = "111111"
            )
            eventTracker.assertContainsEvent(
                "linked_accounts.networking.verification.success",
                mapOf("pane" to "networking_save_to_link_verification")
            )
            navigationManager.assertNavigatedTo(
                destination = Destination.Success,
                pane = Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION
            )
        }

    @Test
    fun `otpEntered - on valid OTP confirms, no accounts but attached ME still saves navigates to success pane`() =
        runTest {
            val linkVerifiedManifest = sessionManifest().copy(nextPane = INSTITUTION_PICKER)
            whenever(getOrFetchSync()).thenReturn(syncResponse(sessionManifest()))
            whenever(markLinkVerified()).thenReturn(linkVerifiedManifest)
            whenever(getCachedAccounts()).thenReturn(emptyList())
            whenever(attachedPaymentAccountRepository.get()).thenReturn(
                AttachedPaymentAccountRepository.State(
                    attachedPaymentAccount = PaymentAccountParams.BankAccount(
                        routingNumber = "123456789",
                        accountNumber = "123456789"
                    )
                )
            )

            val viewModel = buildViewModel()

            val otpController = viewModel.stateFlow.value.payload()!!.otpElement.controller

            // enters valid OTP
            for (i in 0 until otpController.otpLength) {
                otpController.onValueChanged(i, "1")
            }

            val state = viewModel.stateFlow.value
            verify(saveAccountToLink).existing(
                eq(state.payload()!!.consumerSessionClientSecret),
                eq(emptyList()),
                eq(true),
            )
            verify(confirmVerification).sms(
                consumerSessionClientSecret = cachedConsumerSession.clientSecret,
                verificationCode = "111111"
            )
            eventTracker.assertContainsEvent(
                "linked_accounts.networking.verification.success",
                mapOf("pane" to "networking_save_to_link_verification")
            )
            navigationManager.assertNavigatedTo(
                destination = Destination.Success,
                pane = Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION
            )
        }

    @Test
    fun `otpEntered - on valid OTP fails, sends event and navigates to terminal error`() =
        runTest {
            val selectedAccount = cachedPartnerAccount()
            val linkVerifiedManifest = sessionManifest().copy(nextPane = INSTITUTION_PICKER)
            whenever(getOrFetchSync()).thenReturn(syncResponse(sessionManifest()))
            whenever(markLinkVerified()).thenReturn(linkVerifiedManifest)
            whenever(getCachedAccounts()).thenReturn(listOf(selectedAccount))
            whenever(saveAccountToLink.existing(any(), any(), any())).thenThrow(RuntimeException("error"))

            val viewModel = buildViewModel()

            val otpController = viewModel.stateFlow.value.payload()!!.otpElement.controller

            // enters valid OTP
            for (i in 0 until otpController.otpLength) {
                otpController.onValueChanged(i, "1")
            }

            val state = viewModel.stateFlow.value
            verify(saveAccountToLink).existing(
                eq(state.payload()!!.consumerSessionClientSecret),
                eq(listOf(selectedAccount)),
                eq(true),
            )
            verify(confirmVerification).sms(
                consumerSessionClientSecret = cachedConsumerSession.clientSecret,
                verificationCode = "111111"
            )
            eventTracker.assertContainsEvent(
                "linked_accounts.networking.verification.error",
                mapOf(
                    "pane" to "networking_save_to_link_verification",
                    "error" to "ConfirmVerificationSessionError"
                )
            )
        }

    @Test
    fun `onSkipClick - navigates to success without networking accounts`() = runTest {
        val selectedAccount = cachedPartnerAccount()
        val linkVerifiedManifest = sessionManifest()
        whenever(markLinkVerified()).thenReturn(linkVerifiedManifest)
        whenever(getCachedAccounts()).thenReturn(listOf(selectedAccount))

        val viewModel = buildViewModel()

        viewModel.onSkipClick()

        verifyNoInteractions(confirmVerification)
        verifyNoInteractions(saveAccountToLink)
        navigationManager.assertNavigatedTo(
            destination = Destination.Success,
            pane = Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION
        )
    }
}
