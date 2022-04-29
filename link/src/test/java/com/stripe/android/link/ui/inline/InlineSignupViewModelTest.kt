package com.stripe.android.link.ui.inline

import com.google.common.truth.Truth
import com.stripe.android.core.Logger
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.model.ConsumerSession
import com.stripe.android.ui.core.elements.IdentifierSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class InlineSignupViewModelTest {
    private val linkAccountManager = mock<LinkAccountManager>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `When email is provided it should not trigger lookup and should collect phone number`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()
            viewModel.toggleExpanded()
            Truth.assertThat(viewModel.signUpState.value).isEqualTo(SignUpState.InputtingPhone)

            verify(linkAccountManager, times(0)).lookupConsumer(any())
        }

    @Test
    fun `When entered existing account then it becomes ready`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()
            viewModel.toggleExpanded()
            viewModel.emailElement.setRawValue(mapOf(IdentifierSpec.Email to "valid@email.com"))

            val linkAccount = LinkAccount(
                mockConsumerSessionWithVerificationSession(
                    ConsumerSession.VerificationSession.SessionType.Sms,
                    ConsumerSession.VerificationSession.SessionState.Started
                )
            )
            whenever(linkAccountManager.lookupConsumer(any()))
                .thenReturn(Result.success(linkAccount))

            // Advance past lookup debounce delay
            advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE_MS + 100)

            Truth.assertThat(viewModel.isReady.value).isTrue()
        }

    @Test
    fun `When entered non-existing account then it collects phone number`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()
            viewModel.toggleExpanded()
            viewModel.emailElement.setRawValue(mapOf(IdentifierSpec.Email to "valid@email.com"))

            whenever(linkAccountManager.lookupConsumer(any()))
                .thenReturn(Result.success(null))

            // Advance past lookup debounce delay
            advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE_MS + 100)

            Truth.assertThat(viewModel.isReady.value).isFalse()
            Truth.assertThat(viewModel.signUpState.value).isEqualTo(SignUpState.InputtingPhone)
        }

    private fun createViewModel() = InlineSignupViewModel(
        merchantName = MERCHANT_NAME,
        customerEmail = CUSTOMER_EMAIL,
        linkAccountManager = linkAccountManager,
        logger = Logger.noop()
    )

    private fun mockConsumerSessionWithVerificationSession(
        type: ConsumerSession.VerificationSession.SessionType,
        state: ConsumerSession.VerificationSession.SessionState
    ): ConsumerSession {
        val verificationSession = mock<ConsumerSession.VerificationSession>()
        whenever(verificationSession.type).thenReturn(type)
        whenever(verificationSession.state).thenReturn(state)
        val verificationSessions = listOf(verificationSession)

        val consumerSession = mock<ConsumerSession>()
        whenever(consumerSession.verificationSessions).thenReturn(verificationSessions)
        whenever(consumerSession.clientSecret).thenReturn("secret")
        whenever(consumerSession.emailAddress).thenReturn("email")
        return consumerSession
    }

    private companion object {
        const val MERCHANT_NAME = "merchantName"
        const val CUSTOMER_EMAIL = "customer@email.com"
    }
}
