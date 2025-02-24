package com.stripe.android.link.ui.signup

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.exception.stripeErrorMessage
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
import com.stripe.android.paymentsheet.R
import com.stripe.android.testing.CoroutineTestRule
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
        val linkAuth = FakeLinkAuth()
        linkAuth.lookupResult = LinkAuthResult.NoLinkAccountFound

        val viewModel = createViewModel(prefilledEmail = null, linkAuth = linkAuth)

        assertThat(viewModel.contentState.signUpState).isEqualTo(SignUpState.InputtingPrimaryField)

        viewModel.emailController.onRawValueChange("valid@email.com")
        assertThat(viewModel.contentState.signUpState).isEqualTo(SignUpState.InputtingPrimaryField)

        // Advance past lookup debounce delay
        advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

        assertThat(viewModel.emailController.fieldValue.value).isEqualTo("valid@email.com")
        assertThat(viewModel.contentState.signUpState).isEqualTo(SignUpState.InputtingRemainingFields)

        linkAuth.awaitLookupCall()
        linkAuth.ensureAllItemsConsumed()
    }

    @Test
    fun `When email is initially equal to config email, lookup is not triggered`() = runTest(dispatcher) {
        val linkAuth = FakeLinkAuth()
        val viewModel = createViewModel(prefilledEmail = CUSTOMER_EMAIL, linkAuth = linkAuth)

        // No change to email, should not trigger lookup
        advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

        assertThat(viewModel.contentState.signUpState).isEqualTo(SignUpState.InputtingRemainingFields)
        linkAuth.ensureAllItemsConsumed()
    }

    @Test
    fun `When USE_LINK_CONFIGURATION_CUSTOMER_INFO is false, controllers should not be prefilled`() =
        runTest(dispatcher) {
            testUseLinkConfigurationCustomerInfo(
                useLinkConfigurationCustomerInfo = false,
                expectedSignUpState = SignUpState.InputtingPrimaryField,
                expectedEmail = "",
                expectedPhoneNumber = "",
                expectedName = ""
            )
        }

    @Test
    fun `When USE_LINK_CONFIGURATION_CUSTOMER_INFO is true, controllers should be prefilled`() = runTest(dispatcher) {
        testUseLinkConfigurationCustomerInfo(
            useLinkConfigurationCustomerInfo = true,
            expectedSignUpState = SignUpState.InputtingRemainingFields,
            expectedEmail = CUSTOMER_EMAIL,
            expectedPhoneNumber = TestFactory.CUSTOMER_PHONE,
            expectedName = TestFactory.CUSTOMER_NAME
        )
    }

    @Test
    fun `When USE_LINK_CONFIGURATION_CUSTOMER_INFO is not set, controllers should be prefilled`() =
        runTest(dispatcher) {
            testUseLinkConfigurationCustomerInfo(
                useLinkConfigurationCustomerInfo = null,
                expectedSignUpState = SignUpState.InputtingRemainingFields,
                expectedEmail = CUSTOMER_EMAIL,
                expectedPhoneNumber = TestFactory.CUSTOMER_PHONE,
                expectedName = TestFactory.CUSTOMER_NAME
            )
        }

    @Test
    fun `When email changes and then reverts to config email, lookup is triggered`() = runTest(dispatcher) {
        val linkAuth = FakeLinkAuth()
        linkAuth.lookupResult = LinkAuthResult.NoLinkAccountFound

        val viewModel = createViewModel(prefilledEmail = CUSTOMER_EMAIL, linkAuth = linkAuth)

        // Change email
        viewModel.emailController.onRawValueChange("different@email.com")
        advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)
        linkAuth.awaitLookupCall()

        // Revert to original email
        viewModel.emailController.onRawValueChange(CUSTOMER_EMAIL)
        advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)
        linkAuth.awaitLookupCall()
        linkAuth.ensureAllItemsConsumed()
    }

    @Test
    fun `When lookup finds existing account, navigate to appropriate screen`() = runTest(dispatcher) {
        var linkScreen: LinkScreen? = null
        val linkEventsReporter = object : SignUpLinkEventsReporter() {
            override fun onSignupCompleted(isInline: Boolean) {
                calledCount += 1
            }
        }

        val linkAuth = FakeLinkAuth()
        linkAuth.lookupResult = LinkAuthResult.Success(TestFactory.LINK_ACCOUNT)
        val viewModel = createViewModel(
            prefilledEmail = null,
            linkAuth = linkAuth,
            linkEventsReporter = linkEventsReporter,
            navigateAndClearStack = { screen ->
                linkScreen = screen
            }
        )

        viewModel.emailController.onRawValueChange("existing@email.com")
        advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

        assertThat(linkScreen).isEqualTo(LinkScreen.Wallet)
    }

    @Test
    fun `When lookup fails, stay on input remaining fields state`() = runTest(dispatcher) {
        val error = RuntimeException("Lookup failed")
        val linkAuth = FakeLinkAuth()
        linkAuth.lookupResult = LinkAuthResult.Error(error)

        val viewModel = createViewModel(
            prefilledEmail = null,
            linkAuth = linkAuth
        )

        viewModel.emailController.onRawValueChange("valid@email.com")
        advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

        assertThat(viewModel.state.value.signUpState).isEqualTo(SignUpState.InputtingRemainingFields)
        assertThat(viewModel.state.value.errorMessage).isEqualTo(error.stripeErrorMessage())
    }

    @Test
    fun `When lookup fails with account error, stay on input remaining fields state`() = runTest(dispatcher) {
        val error = RuntimeException("Lookup failed")
        val linkAuth = FakeLinkAuth()
        val logger = FakeLogger()
        linkAuth.lookupResult = LinkAuthResult.AccountError(error)

        val viewModel = createViewModel(
            prefilledEmail = null,
            linkAuth = linkAuth,
            logger = logger
        )

        viewModel.emailController.onRawValueChange("valid@email.com")
        advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

        assertThat(viewModel.state.value.signUpState).isEqualTo(SignUpState.InputtingPrimaryField)
        assertThat(viewModel.state.value.errorMessage)
            .isEqualTo(R.string.stripe_signup_deactivated_account_message.resolvableString)
        assertThat(logger.errorLogs).containsExactly("SignUpViewModel Error: " to error)
    }

    @Test
    fun `When email is provided it should not trigger lookup and should collect remaining fields`() =
        runTest(dispatcher) {
            val linkAuth = FakeLinkAuth()
            linkAuth.lookupResult = LinkAuthResult.NoLinkAccountFound
            val viewModel = createViewModel(prefilledEmail = CUSTOMER_EMAIL, linkAuth = linkAuth)

            advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

            assertThat(viewModel.state.value.signUpState).isEqualTo(SignUpState.InputtingRemainingFields)
            linkAuth.ensureAllItemsConsumed()
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
        linkAuth.lookupResult = LinkAuthResult.NoLinkAccountFound

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

        assertThat(viewModel.contentState.errorMessage).isEqualTo(exception.stripeErrorMessage())
        assertThat(logger.errorLogs).isEqualTo(listOf("SignUpViewModel Error: " to exception))
    }

    @Test
    fun `When signUp fails with account error then an error message is shown`() = runTest(dispatcher) {
        val errorMessage = "Error message"

        val linkAuth = FakeLinkAuth()
        linkAuth.lookupResult = LinkAuthResult.NoLinkAccountFound

        val logger = FakeLogger()
        val viewModel = createViewModel(
            linkAuth = linkAuth,
            linkEventsReporter = object : SignUpLinkEventsReporter() {
                override fun onSignupFailure(isInline: Boolean, error: Throwable) = Unit
            },
            logger = logger
        )

        val exception = RuntimeException(errorMessage)
        linkAuth.signupResult = LinkAuthResult.AccountError(exception)

        viewModel.performValidSignup()

        assertThat(viewModel.state.value.errorMessage)
            .isEqualTo(R.string.stripe_signup_deactivated_account_message.resolvableString)
        assertThat(viewModel.contentState.signUpState).isEqualTo(SignUpState.InputtingPrimaryField)
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
        linkAuth.lookupResult = LinkAuthResult.NoLinkAccountFound

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

    @Test
    fun `attestation error on lookup calls moveToWeb`() = runTest(dispatcher) {
        val linkAuth = FakeLinkAuth()
        linkAuth.lookupResult = LinkAuthResult.AttestationFailed(Throwable())

        var movedToWeb = false

        val viewModel = createViewModel(
            prefilledEmail = null,
            linkAuth = linkAuth,
            moveToWeb = {
                movedToWeb = true
            }
        )

        viewModel.emailController.onRawValueChange("a@b.com")

        // Advance past lookup debounce delay
        advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

        assertThat(movedToWeb).isTrue()
    }

    @Test
    fun `generic lookup error does not moveToWeb`() = runTest(dispatcher) {
        val linkAuth = FakeLinkAuth()
        linkAuth.lookupResult = LinkAuthResult.Error(Throwable())

        var movedToWeb = false
        val viewModel = createViewModel(
            prefilledEmail = CUSTOMER_EMAIL,
            linkAuth = linkAuth,
            moveToWeb = {
                movedToWeb = true
            }
        )

        assertThat(movedToWeb).isFalse()
    }

    @Test
    fun `attestation error on sign up calls moveToWeb`() = runTest(dispatcher) {
        val linkAuth = FakeLinkAuth()
        linkAuth.lookupResult = LinkAuthResult.NoLinkAccountFound
        linkAuth.signupResult = LinkAuthResult.AttestationFailed(Throwable())

        var movedToWeb = false
        val viewModel = createViewModel(
            prefilledEmail = null,
            linkAuth = linkAuth,
            moveToWeb = {
                movedToWeb = true
            }
        )

        viewModel.performValidSignup()

        assertThat(movedToWeb).isTrue()
    }

    @Test
    fun `generic sign up error does not call moveToWeb`() = runTest(dispatcher) {
        val linkAuth = FakeLinkAuth()
        linkAuth.lookupResult = LinkAuthResult.Error(Throwable())

        var movedToWeb = false
        val viewModel = createViewModel(
            prefilledEmail = null,
            linkAuth = linkAuth,
            moveToWeb = {
                movedToWeb = true
            }
        )

        viewModel.performValidSignup()

        assertThat(movedToWeb).isFalse()
    }

    private fun SignUpViewModel.performValidSignup() {
        emailController.onRawValueChange("email@valid.co")
        phoneNumberController.onRawValueChange("1234567890")
        onSignUpClick()
    }

    private fun testUseLinkConfigurationCustomerInfo(
        useLinkConfigurationCustomerInfo: Boolean?,
        expectedSignUpState: SignUpState = SignUpState.InputtingRemainingFields,
        expectedEmail: String = CUSTOMER_EMAIL,
        expectedPhoneNumber: String = TestFactory.CUSTOMER_PHONE,
        expectedName: String = TestFactory.CUSTOMER_NAME
    ) {
        val savedStateHandle = SavedStateHandle()
            .apply {
                useLinkConfigurationCustomerInfo?.let {
                    set(SignUpViewModel.USE_LINK_CONFIGURATION_CUSTOMER_INFO, it)
                }
            }
        val viewModel = createViewModel(
            prefilledEmail = CUSTOMER_EMAIL,
            savedStateHandle = savedStateHandle
        )

        dispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.contentState.signUpState).isEqualTo(expectedSignUpState)
        assertThat(viewModel.emailController.fieldValue.value).isEqualTo(expectedEmail)
        assertThat(viewModel.phoneNumberController.fieldValue.value).isEqualTo(expectedPhoneNumber)
        assertThat(viewModel.nameController.fieldValue.value).isEqualTo(expectedName)
    }

    private fun createViewModel(
        prefilledEmail: String? = null,
        configuration: LinkConfiguration = TestFactory.LINK_CONFIGURATION,
        countryCode: CountryCode = CountryCode.US,
        linkEventsReporter: LinkEventsReporter = SignUpLinkEventsReporter(),
        linkAuth: LinkAuth = FakeLinkAuth().apply {
            lookupResult = LinkAuthResult.NoLinkAccountFound
        },
        logger: Logger = FakeLogger(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
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
            savedStateHandle = savedStateHandle,
            navigate = navigate,
            navigateAndClearStack = navigateAndClearStack,
            moveToWeb = moveToWeb,
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

    override fun onSignupCompleted(isInline: Boolean) = Unit
}
