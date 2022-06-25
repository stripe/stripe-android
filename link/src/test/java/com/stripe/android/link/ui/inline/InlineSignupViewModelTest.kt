package com.stripe.android.link.ui.inline

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.model.ConsumerSession
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
            assertThat(viewModel.signUpState.value).isEqualTo(SignUpState.InputtingPhone)

            verify(linkAccountManager, times(0)).lookupConsumer(any(), any())
        }

    @Test
    fun `When email and phone are provided it should prefill all values`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = InlineSignupViewModel(
                merchantName = MERCHANT_NAME,
                customerEmail = CUSTOMER_EMAIL,
                customerPhone = CUSTOMER_PHONE,
                linkAccountManager = linkAccountManager,
                logger = Logger.noop()
            )
            viewModel.toggleExpanded()
            assertThat(viewModel.signUpState.value).isEqualTo(SignUpState.InputtingPhone)
            assertThat(viewModel.phoneController.initialPhoneNumber).isEqualTo(CUSTOMER_PHONE)

            verify(linkAccountManager, times(0)).lookupConsumer(any(), any())
        }

    @Test
    fun `When entered existing account then it emits user input`() =
        runTest(UnconfinedTestDispatcher()) {
            val email = "valid@email.com"
            val viewModel = createViewModel()
            viewModel.toggleExpanded()
            viewModel.emailController.onRawValueChange(email)

            val linkAccount = LinkAccount(
                mockConsumerSessionWithVerificationSession(
                    ConsumerSession.VerificationSession.SessionType.Sms,
                    ConsumerSession.VerificationSession.SessionState.Started
                )
            )
            whenever(linkAccountManager.lookupConsumer(any(), any()))
                .thenReturn(Result.success(linkAccount))

            // Advance past lookup debounce delay
            advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE_MS + 100)

            assertThat(viewModel.userInput.value).isEqualTo(UserInput.SignIn(email))
        }

    @Test
    fun `When entered non-existing account then it collects phone number`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()
            viewModel.toggleExpanded()
            viewModel.emailController.onRawValueChange("valid@email.com")

            whenever(linkAccountManager.lookupConsumer(any(), any()))
                .thenReturn(Result.success(null))

            // Advance past lookup debounce delay
            advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE_MS + 100)

            assertThat(viewModel.userInput.value).isNull()
            assertThat(viewModel.signUpState.value).isEqualTo(SignUpState.InputtingPhone)
        }

    @Test
    fun `When entered all fields for new account then it emits user input`() =
        runTest(UnconfinedTestDispatcher()) {
            val email = "valid@email.com"
            val viewModel = createViewModel()
            viewModel.toggleExpanded()
            viewModel.emailController.onRawValueChange(email)

            assertThat(viewModel.userInput.value).isNull()

            whenever(linkAccountManager.lookupConsumer(any(), any()))
                .thenReturn(Result.success(null))

            // Advance past lookup debounce delay
            advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE_MS + 100)

            assertThat(viewModel.userInput.value).isNull()

            val phone = "1234567890"
            viewModel.phoneController.onRawValueChange(phone)

            assertThat(viewModel.userInput.value)
                .isEqualTo(UserInput.SignUp(email, "+1$phone", "US"))
        }

    @Test
    fun `When user input becomes invalid then it emits null user input`() =
        runTest(UnconfinedTestDispatcher()) {
            val email = "valid@email.com"
            val viewModel = createViewModel()
            viewModel.toggleExpanded()
            viewModel.emailController.onRawValueChange(email)

            assertThat(viewModel.userInput.value).isNull()

            whenever(linkAccountManager.lookupConsumer(any(), any()))
                .thenReturn(Result.success(null))

            // Advance past lookup debounce delay
            advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE_MS + 100)

            assertThat(viewModel.userInput.value).isNull()

            val phone = "1234567890"
            viewModel.phoneController.onRawValueChange(phone)

            assertThat(viewModel.userInput.value)
                .isEqualTo(UserInput.SignUp(email, "+1$phone", "US"))

            viewModel.phoneController.onRawValueChange("")

            assertThat(viewModel.userInput.value).isNull()
        }

    private fun createViewModel() = InlineSignupViewModel(
        merchantName = MERCHANT_NAME,
        customerEmail = CUSTOMER_EMAIL,
        customerPhone = null,
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
        const val CUSTOMER_PHONE = "1234567890"
    }
}
