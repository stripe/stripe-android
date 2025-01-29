package com.stripe.android.link.ui.signup

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.TestFactory
import com.stripe.android.link.TestFactory.CUSTOMER_EMAIL
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.FakeLinkAuth
import com.stripe.android.link.account.LinkAuth
import com.stripe.android.link.account.LinkAuthResult
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.FakeLogger
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.milliseconds

@RunWith(RobolectricTestRunner::class)
internal class SignUpViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Test
    fun `init sends analytics event`() = runTest(dispatcher) {
        val linkEventsReporter = object : SignUpLinkEventsReporter() {
            override fun onSignupFlowPresented() {
                calledCount += 1
            }
        }

        createViewModel(linkEventsReporter = linkEventsReporter)

        assertThat(linkEventsReporter.calledCount).isEqualTo(1)
    }

    @Test
    fun `When email is valid then lookup is triggered with delay`() = runTest(dispatcher) {
        val viewModel = createViewModel(prefilledEmail = null)

        assertThat(viewModel.contentState.signUpState).isEqualTo(SignUpState.InputtingPrimaryField)

        viewModel.emailController.onRawValueChange("valid@email.com")
        assertThat(viewModel.contentState.signUpState).isEqualTo(SignUpState.InputtingPrimaryField)

        // Advance past lookup debounce delay
        advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

        assertThat(viewModel.emailController.fieldValue.value).isEqualTo("valid@email.com")
        assertThat(viewModel.contentState.signUpState).isEqualTo(SignUpState.InputtingRemainingFields)
    }

    @Test
    fun `When email is provided it should not trigger lookup and should collect phone number`() = runTest(dispatcher) {
        val linkAuth = FakeLinkAuth()
        val viewModel = createViewModel(prefilledEmail = CUSTOMER_EMAIL, linkAuth = linkAuth)

        assertThat(viewModel.contentState.signUpState).isEqualTo(SignUpState.InputtingPrimaryField)
        linkAuth.ensureAllItemsConsumed()
    }

    @Test
    fun `signUp sends correct ConsumerSignUpConsentAction`() = runTest(dispatcher) {
        val linkAuth = FakeLinkAuth()
        val viewModel = createViewModel(
            linkAuth = linkAuth,
            linkEventsReporter = object : SignUpLinkEventsReporter() {
                override fun onSignupCompleted(isInline: Boolean) = Unit
            }
        )

        viewModel.performValidSignup()

        val call = linkAuth.awaitSignUpCall()
        assertThat(call.consentAction).isEqualTo(SignUpConsentAction.Implied)
        linkAuth.ensureAllItemsConsumed()
    }

    @Test
    fun `When signUp fails then an error message is shown`() = runTest(dispatcher) {
        val errorMessage = "Error message"

        val linkAuth = FakeLinkAuth()
        val logger = FakeLogger()
        val viewModel = createViewModel(
            linkAuth = linkAuth,
            linkEventsReporter = object : SignUpLinkEventsReporter() {
                override fun onSignupFailure(isInline: Boolean, error: Throwable) = Unit
            },
            logger = logger
        )

        val exception = RuntimeException(errorMessage)
        linkAuth.signupResult = LinkAuthResult.Error(exception)

        viewModel.performValidSignup()

        assertThat(viewModel.contentState.errorMessage).isEqualTo(errorMessage.resolvableString)
        assertThat(logger.errorLogs).isEqualTo(listOf("SignUpViewModel Error: " to exception))
    }

    @Test
    fun `When signed up with unverified account then it navigates to Verification screen`() = runTest(dispatcher) {
        val screens = arrayListOf<LinkScreen>()
        val linkAuth = FakeLinkAuth()
        val viewModel = createViewModel(
            linkAuth = linkAuth,
            linkEventsReporter = object : SignUpLinkEventsReporter() {
                override fun onSignupCompleted(isInline: Boolean) = Unit
            },
            navigate = { screen ->
                screens.add(screen)
            }
        )

        val linkAccount = LinkAccount(
            mockConsumerSessionWithVerificationSession(
                ConsumerSession.VerificationSession.SessionType.Sms,
                ConsumerSession.VerificationSession.SessionState.Started
            )
        )
        linkAuth.signupResult = LinkAuthResult.Success(linkAccount)

        viewModel.performValidSignup()

        assertThat(screens).containsExactly(LinkScreen.Verification)
        assertThat(viewModel.contentState.signUpState).isEqualTo(SignUpState.InputtingPrimaryField)
    }

    @Test
    fun `When signed up with verified account then it navigates to Wallet screen`() = runTest(dispatcher) {
        val screens = arrayListOf<LinkScreen>()
        val linkAuth = FakeLinkAuth()
        val viewModel = createViewModel(
            linkAuth = linkAuth,
            linkEventsReporter = object : SignUpLinkEventsReporter() {
                override fun onSignupCompleted(isInline: Boolean) = Unit
            },
            navigateAndClearStack = { screen ->
                screens.add(screen)
            }
        )

        val linkAccount = LinkAccount(
            mockConsumerSessionWithVerificationSession(
                ConsumerSession.VerificationSession.SessionType.Sms,
                ConsumerSession.VerificationSession.SessionState.Verified
            )
        )

        linkAuth.signupResult = LinkAuthResult.Success(linkAccount)

        viewModel.performValidSignup()

        assertThat(screens).containsExactly(LinkScreen.Wallet)
    }

    @Test
    fun `When signup succeeds then analytics event is sent`() = runTest(dispatcher) {
        val linkEventsReporter = object : SignUpLinkEventsReporter() {
            override fun onSignupCompleted(isInline: Boolean) {
                calledCount += 1
            }
        }
        val linkAccountManager = FakeLinkAccountManager()

        val viewModel = createViewModel(
            linkEventsReporter = linkEventsReporter
        )

        val linkAccount = LinkAccount(
            mockConsumerSessionWithVerificationSession(
                ConsumerSession.VerificationSession.SessionType.Sms,
                ConsumerSession.VerificationSession.SessionState.Verified
            )
        )
        linkAccountManager.signUpResult = Result.success(linkAccount)

        viewModel.performValidSignup()

        assertThat(linkEventsReporter.calledCount).isEqualTo(1)
    }

    @Test
    fun `When signup fails then analytics event is sent`() = runTest(dispatcher) {
        val expectedError = Exception()

        val linkEventsReporter = object : SignUpLinkEventsReporter() {
            override fun onSignupFailure(isInline: Boolean, error: Throwable) {
                if (!isInline && expectedError == error) {
                    calledCount += 1
                }
            }
        }

        val linkAuth = FakeLinkAuth()

        val viewModel = createViewModel(
            linkEventsReporter = linkEventsReporter,
            linkAuth = linkAuth
        )

        linkAuth.signupResult = LinkAuthResult.Error(expectedError)

        viewModel.performValidSignup()

        assertThat(linkEventsReporter.calledCount).isEqualTo(1)
    }

    @Test
    fun `Doesn't require name for US consumers`() = runTest(dispatcher) {
        val viewModel = createViewModel(
            prefilledEmail = null,
            countryCode = CountryCode.US
        )

        assertThat(viewModel.contentState.signUpEnabled).isFalse()

        viewModel.emailController.onRawValueChange("me@myself.com")
        viewModel.phoneNumberController.onRawValueChange("1234567890")
        assertThat(viewModel.contentState.signUpEnabled).isTrue()
    }

    @Test
    fun `Requires name for non-US consumers`() = runTest(dispatcher) {
        val viewModel = createViewModel(
            prefilledEmail = null,
            countryCode = CountryCode.CA
        )

        assertThat(viewModel.contentState.signUpEnabled).isFalse()

        viewModel.emailController.onRawValueChange("me@myself.com")
        viewModel.phoneNumberController.onRawValueChange("1234567890")
        viewModel.nameController.onRawValueChange("")
        assertThat(viewModel.contentState.signUpEnabled).isFalse()

        viewModel.nameController.onRawValueChange("Someone from Canada")
        assertThat(viewModel.contentState.signUpEnabled).isTrue()
    }

    @Test
    fun `Prefilled values are handled correctly`() = runTest(dispatcher) {
        val viewModel = createViewModel(
            prefilledEmail = CUSTOMER_EMAIL,
            countryCode = CountryCode.US
        )

        assertThat(viewModel.contentState.signUpEnabled).isTrue()
    }

    private fun SignUpViewModel.performValidSignup() {
        emailController.onRawValueChange("email@valid.co")
        phoneNumberController.onRawValueChange("1234567890")
        onSignUpClick()
    }

    private fun createViewModel(
        prefilledEmail: String? = null,
        configuration: LinkConfiguration = TestFactory.LINK_CONFIGURATION,
        countryCode: CountryCode = CountryCode.US,
        linkEventsReporter: LinkEventsReporter = SignUpLinkEventsReporter(),
        linkAuth: LinkAuth = FakeLinkAuth(),
        logger: Logger = FakeLogger(),
        errorReporter: ErrorReporter = FakeErrorReporter(),
        navigate: (LinkScreen) -> Unit = {},
        navigateAndClearStack: (LinkScreen) -> Unit = {},
        moveToWeb: () -> Unit = {}
    ): SignUpViewModel {
        return SignUpViewModel(
            configuration = configuration.copy(
                customerInfo = configuration.customerInfo.copy(
                    email = prefilledEmail,
                ),
                stripeIntent = when (val intent = configuration.stripeIntent) {
                    is PaymentIntent -> intent.copy(countryCode = countryCode.value)
                    is SetupIntent -> intent.copy(countryCode = countryCode.value)
                }
            ),
            linkAuth = linkAuth,
            linkEventsReporter = linkEventsReporter,
            logger = logger,
            navigate = navigate,
            navigateAndClearStack = navigateAndClearStack,
            moveToWeb = moveToWeb,
            errorReporter = errorReporter
        )
    }

    private fun mockConsumerSessionWithVerificationSession(
        type: ConsumerSession.VerificationSession.SessionType,
        state: ConsumerSession.VerificationSession.SessionState
    ): ConsumerSession {
        return ConsumerSession(
            emailAddress = "",
            redactedPhoneNumber = "",
            verificationSessions = listOf(
                ConsumerSession.VerificationSession(
                    type = type,
                    state = state
                )
            ),
            redactedFormattedPhoneNumber = ""
        )
    }

    private val SignUpViewModel.contentState: SignUpScreenState
        get() = state.value
}

private open class SignUpLinkEventsReporter : FakeLinkEventsReporter() {
    override fun onSignupFlowPresented() = Unit

    override fun onSignupStarted(isInline: Boolean) = Unit
}
