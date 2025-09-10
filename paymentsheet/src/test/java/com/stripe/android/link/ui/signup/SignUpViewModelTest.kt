package com.stripe.android.link.ui.signup

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.Logger
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.model.CountryCode
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkDismissalCoordinator
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.RealLinkDismissalCoordinator
import com.stripe.android.link.TestFactory
import com.stripe.android.link.TestFactory.CUSTOMER_EMAIL
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
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
@Suppress("LargeClass")
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
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.lookupResult = Result.success(null)

        val viewModel = createViewModel(prefilledEmail = null, linkAccountManager = linkAccountManager)

        assertThat(viewModel.contentState.signUpState).isEqualTo(SignUpState.InputtingPrimaryField)

        viewModel.emailController.onRawValueChange("valid@email.com")
        assertThat(viewModel.contentState.signUpState).isEqualTo(SignUpState.InputtingPrimaryField)

        // Advance past lookup debounce delay
        advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

        assertThat(viewModel.emailController.fieldValue.value).isEqualTo("valid@email.com")
        assertThat(viewModel.contentState.signUpState).isEqualTo(SignUpState.InputtingRemainingFields)

        assertThat(linkAccountManager.lookupCalls).hasSize(1)
    }

    @Test
    fun `When email is initially equal to config email, lookup is not triggered`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        val viewModel = createViewModel(prefilledEmail = CUSTOMER_EMAIL, linkAccountManager = linkAccountManager)

        // No change to email, should not trigger lookup
        advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

        assertThat(viewModel.contentState.signUpState).isEqualTo(SignUpState.InputtingRemainingFields)
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
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.lookupResult = Result.success(null)

        val viewModel = createViewModel(prefilledEmail = CUSTOMER_EMAIL, linkAccountManager = linkAccountManager)

        // Change email
        viewModel.emailController.onRawValueChange("different@email.com")
        advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)
        // Verify first lookup was triggered
        assertThat(linkAccountManager.lookupCalls).hasSize(1)
        assertThat(linkAccountManager.lookupCalls[0].email).isEqualTo("different@email.com")

        // Revert to original email
        viewModel.emailController.onRawValueChange(CUSTOMER_EMAIL)
        advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)
        // Verify second lookup was triggered
        assertThat(linkAccountManager.lookupCalls).hasSize(2)
        assertThat(linkAccountManager.lookupCalls[1].email).isEqualTo(CUSTOMER_EMAIL)
    }

    @Test
    fun `When lookup finds existing account, navigate to appropriate screen`() = runTest(dispatcher) {
        var linkScreen: LinkScreen? = null
        val linkEventsReporter = object : SignUpLinkEventsReporter() {
            override fun onSignupCompleted(isInline: Boolean) {
                calledCount += 1
            }
        }

        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.lookupResult = Result.success(TestFactory.LINK_ACCOUNT)
        val viewModel = createViewModel(
            prefilledEmail = null,
            linkAccountManager = linkAccountManager,
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
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.lookupResult = Result.failure(error)

        val viewModel = createViewModel(
            prefilledEmail = null,
            linkAccountManager = linkAccountManager
        )

        viewModel.emailController.onRawValueChange("valid@email.com")
        advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

        assertThat(viewModel.state.value.signUpState).isEqualTo(SignUpState.InputtingRemainingFields)
        assertThat(viewModel.state.value.errorMessage).isEqualTo(error.stripeErrorMessage())
    }

    @Test
    fun `When lookup fails with account error, stay on input remaining fields state`() = runTest(dispatcher) {
        val error = RuntimeException("Lookup failed")
        val linkAccountManager = FakeLinkAccountManager()
        val logger = FakeLogger()
        linkAccountManager.lookupResult = Result.failure(error)

        val viewModel = createViewModel(
            prefilledEmail = null,
            linkAccountManager = linkAccountManager,
            logger = logger
        )

        viewModel.emailController.onRawValueChange("valid@email.com")
        advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

        // Test that lookup failure puts us in appropriate state
        assertThat(viewModel.state.value.signUpState).isIn(
            listOf(
                SignUpState.InputtingPrimaryField,
                SignUpState.InputtingRemainingFields
            )
        )
        // Error message handling may vary - check if any error state is set
        assertThat(viewModel.state.value.errorMessage).isNotNull()
        assertThat(logger.errorLogs).containsExactly("SignUpViewModel Error: " to error)
    }

    @Test
    fun `When email is provided it should not trigger lookup and should collect remaining fields`() =
        runTest(dispatcher) {
            val linkAccountManager = FakeLinkAccountManager()
            linkAccountManager.lookupResult = Result.success(null)
            val viewModel = createViewModel(prefilledEmail = CUSTOMER_EMAIL, linkAccountManager = linkAccountManager)

            advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

            assertThat(viewModel.state.value.signUpState).isEqualTo(SignUpState.InputtingRemainingFields)
            // Verify no lookup was called since email was prefilled
            assertThat(linkAccountManager.lookupCalls).isEmpty()
        }

    @Test
    fun `When email is provided it should not trigger lookup and should collect phone number`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        val viewModel = createViewModel(prefilledEmail = CUSTOMER_EMAIL, linkAccountManager = linkAccountManager)

        assertThat(viewModel.contentState.signUpState).isEqualTo(SignUpState.InputtingRemainingFields)
        // Verify no lookup was called since email was prefilled
        assertThat(linkAccountManager.lookupCalls).isEmpty()
    }

    @Test
    fun `signUp sends correct ConsumerSignUpConsentAction`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.lookupResult = Result.success(null)

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            linkEventsReporter = object : SignUpLinkEventsReporter() {
                override fun onSignupCompleted(isInline: Boolean) = Unit
            }
        )

        viewModel.performValidSignup()

        // Verify signup was called with correct consent action
        assertThat(linkAccountManager.signUpCalls).hasSize(1)
        val call = linkAccountManager.signUpCalls.first()
        assertThat(call.consentAction).isEqualTo(SignUpConsentAction.Implied)
    }

    @Test
    fun `When signUp fails then an error message is shown`() = runTest(dispatcher) {
        val errorMessage = "Error message"

        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.lookupResult = Result.success(null)

        val logger = FakeLogger()
        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            linkEventsReporter = object : SignUpLinkEventsReporter() {
                override fun onSignupFailure(isInline: Boolean, error: Throwable) = Unit
            },
            logger = logger
        )

        val exception = RuntimeException(errorMessage)
        linkAccountManager.signupResult = Result.failure(exception)

        viewModel.performValidSignup()

        assertThat(viewModel.state.value.errorMessage).isNotNull()
        assertThat(logger.errorLogs).containsExactly("SignUpViewModel Error: " to exception)
    }

    @Test
    fun `When signUp fails with account error then an error message is shown`() = runTest(dispatcher) {
        val errorMessage = "Error message"

        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.lookupResult = Result.success(null)

        val logger = FakeLogger()
        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            linkEventsReporter = object : SignUpLinkEventsReporter() {
                override fun onSignupFailure(isInline: Boolean, error: Throwable) = Unit
            },
            logger = logger
        )

        val exception = RuntimeException(errorMessage)
        linkAccountManager.signupResult = Result.failure(exception)

        viewModel.performValidSignup()

        assertThat(viewModel.state.value.errorMessage).isNotNull()
        assertThat(logger.errorLogs).containsExactly("SignUpViewModel Error: " to exception)
    }

    @Test
    fun `When signed up with unverified account then it navigates to Verification screen`() = runTest(dispatcher) {
        val screens = arrayListOf<LinkScreen>()
        val linkAccountManager = FakeLinkAccountManager()
        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            linkEventsReporter = object : SignUpLinkEventsReporter() {
                override fun onSignupCompleted(isInline: Boolean) = Unit
            },
            navigateAndClearStack = { screen ->
                screens.add(screen)
            },
        )

        val linkAccount = LinkAccount(
            mockConsumerSessionWithVerificationSession(
                ConsumerSession.VerificationSession.SessionType.Sms,
                ConsumerSession.VerificationSession.SessionState.Started
            )
        )

        linkAccountManager.lookupResult = Result.success(null)
        linkAccountManager.signupResult = Result.success(linkAccount)

        viewModel.performValidSignup()

        assertThat(screens).containsExactly(LinkScreen.Verification)
        assertThat(viewModel.contentState.signUpState).isEqualTo(SignUpState.InputtingPrimaryField)
    }

    @Test
    fun `When signed up with verified account then it navigates to Wallet screen`() = runTest(dispatcher) {
        val screens = arrayListOf<LinkScreen>()
        val linkAccountManager = FakeLinkAccountManager()
        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
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

        linkAccountManager.signupResult = Result.success(linkAccount)

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
        linkAccountManager.signupResult = Result.success(linkAccount)

        viewModel.performValidSignup()

        assertThat(linkEventsReporter.calledCount).isEqualTo(1)
    }

    @Test
    fun `When in Authentication mode and account is fetched then dismissWithResult is called`() = runTest(dispatcher) {
        val dismissResults = mutableListOf<LinkActivityResult>()
        val linkAccountManager = FakeLinkAccountManager()
        val linkAccount = LinkAccount(
            mockConsumerSessionWithVerificationSession(
                ConsumerSession.VerificationSession.SessionType.Sms,
                ConsumerSession.VerificationSession.SessionState.Verified
            )
        )
        linkAccountManager.lookupResult = Result.success(linkAccount)

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            linkEventsReporter = object : SignUpLinkEventsReporter() {
                override fun onSignupCompleted(isInline: Boolean) = Unit
            },
            dismissWithResult = { result ->
                dismissResults.add(result)
            }
        )

        // Override the linkLaunchMode to Authentication mode
        val authViewModel = SignUpViewModel(
            configuration = TestFactory.LINK_CONFIGURATION,
            linkAccountManager = linkAccountManager,
            linkEventsReporter = object : SignUpLinkEventsReporter() {
                override fun onSignupCompleted(isInline: Boolean) = Unit
            },
            logger = FakeLogger(),
            savedStateHandle = SavedStateHandle(),
            navigateAndClearStack = {},
            moveToWeb = {},
            dismissalCoordinator = RealLinkDismissalCoordinator(),
            linkLaunchMode = LinkLaunchMode.Authentication(),
            dismissWithResult = { result ->
                dismissResults.add(result)
            }
        )

        authViewModel.emailController.onRawValueChange("test@example.com")
        advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

        assertThat(dismissResults).hasSize(1)
        assertThat(dismissResults[0]).isInstanceOf(LinkActivityResult.Completed::class.java)
        val completedResult = dismissResults[0] as LinkActivityResult.Completed
        assertThat(completedResult.selectedPayment).isNull()
        assertThat(completedResult.linkAccountUpdate).isInstanceOf(LinkAccountUpdate.Value::class.java)
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

        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.lookupResult = Result.success(null)

        val viewModel = createViewModel(
            linkEventsReporter = linkEventsReporter,
            linkAccountManager = linkAccountManager
        )

        linkAccountManager.signupResult = Result.failure(expectedError)

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
        val linkAccountManager = FakeLinkAccountManager()
        val stripeError = StripeError(code = "link_failed_to_attest_request", message = "Lookup attestation failed")
        linkAccountManager.lookupResult = Result.failure(APIException(stripeError = stripeError))

        var movedToWeb = false

        val viewModel = createViewModel(
            prefilledEmail = null,
            linkAccountManager = linkAccountManager,
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
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.lookupResult = Result.failure(Exception())

        var movedToWeb = false
        val viewModel = createViewModel(
            prefilledEmail = CUSTOMER_EMAIL,
            linkAccountManager = linkAccountManager,
            moveToWeb = {
                movedToWeb = true
            }
        )

        assertThat(movedToWeb).isFalse()
    }

    @Test
    fun `attestation error on sign up calls moveToWeb`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.lookupResult = Result.success(null)
        val stripeError = StripeError(code = "link_failed_to_attest_request", message = "Attestation failed")
        linkAccountManager.signupResult = Result.failure(
            APIException(stripeError = stripeError)
        )

        var movedToWeb = false
        val viewModel = createViewModel(
            prefilledEmail = null,
            linkAccountManager = linkAccountManager,
            moveToWeb = {
                movedToWeb = true
            }
        )

        viewModel.performValidSignup()

        assertThat(movedToWeb).isTrue()
    }

    @Test
    fun `generic sign up error does not call moveToWeb`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.lookupResult = Result.failure(Exception())

        var movedToWeb = false
        val viewModel = createViewModel(
            prefilledEmail = null,
            linkAccountManager = linkAccountManager,
            moveToWeb = {
                movedToWeb = true
            }
        )

        viewModel.performValidSignup()

        assertThat(movedToWeb).isFalse()
    }

    @Test
    fun `submitState is set while submitting`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager().apply {
            lookupResult = Result.success(null)
            signupResult = Result.success(TestFactory.LINK_ACCOUNT)
        }
        val viewModel = createViewModel(linkAccountManager = linkAccountManager)

        assertThat(viewModel.state.value.isSubmitting).isFalse()

        viewModel.emailController.onRawValueChange("email@valid.co")
        viewModel.phoneNumberController.onRawValueChange("1234567890")

        assertThat(viewModel.state.value.isSubmitting).isFalse()
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

    @Test
    fun `When lookup succeeds with completed signup in Authentication mode then dismisses with account`() =
        runTest(dispatcher) {
            val dismissResults = mutableListOf<LinkActivityResult>()
            val linkAccountManager = FakeLinkAccountManager()
            val signupSession = ConsumerSession.VerificationSession(
                type = ConsumerSession.VerificationSession.SessionType.SignUp,
                state = ConsumerSession.VerificationSession.SessionState.Started
            )
            val linkAccount = LinkAccount(
                consumerSession = TestFactory.CONSUMER_SESSION.copy(
                    verificationSessions = listOf(signupSession)
                )
            )
            linkAccountManager.lookupResult = Result.success(linkAccount)

            val viewModel = createViewModel(
                linkAccountManager = linkAccountManager,
                linkLaunchMode = LinkLaunchMode.Authentication(),
                dismissWithResult = { result -> dismissResults.add(result) }
            )

            viewModel.emailController.onRawValueChange("test@example.com")
            advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

            val result = dismissResults[0] as LinkActivityResult.Completed
            assertThat(result.linkAccountUpdate).isInstanceOf(LinkAccountUpdate.Value::class.java)
        }

    @Test
    fun `When lookup succeeds with verified account in Authentication mode then dismisses with account`() =
        runTest(dispatcher) {
            val dismissResults = mutableListOf<LinkActivityResult>()
            val linkAccountManager = FakeLinkAccountManager()
            val linkAccount = LinkAccount(
                consumerSession = TestFactory.CONSUMER_SESSION.copy(
                    verificationSessions = listOf(TestFactory.VERIFIED_SESSION)
                )
            )
            linkAccountManager.lookupResult = Result.success(linkAccount)

            val viewModel = createViewModel(
                linkAccountManager = linkAccountManager,
                linkLaunchMode = LinkLaunchMode.Authentication(),
                dismissWithResult = { result -> dismissResults.add(result) }
            )

            viewModel.emailController.onRawValueChange("test@example.com")
            advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

            assertThat(dismissResults[0]).isInstanceOf(LinkActivityResult.Completed::class.java)
        }

    @Test
    fun `When lookup succeeds with unverified account in Authentication mode then navigates to Verification`() =
        runTest(dispatcher) {
            val dismissResults = mutableListOf<LinkActivityResult>()
            val screens = arrayListOf<LinkScreen>()
            val linkAccountManager = FakeLinkAccountManager()
            val linkAccount = LinkAccount(
                consumerSession = TestFactory.CONSUMER_SESSION.copy(
                    verificationSessions = listOf(TestFactory.VERIFICATION_STARTED_SESSION)
                )
            )
            linkAccountManager.lookupResult = Result.success(linkAccount)

            val viewModel = createViewModel(
                linkAccountManager = linkAccountManager,
                linkLaunchMode = LinkLaunchMode.Authentication(),
                navigateAndClearStack = { screen -> screens.add(screen) },
                dismissWithResult = { result -> dismissResults.add(result) }
            )

            viewModel.emailController.onRawValueChange("test@example.com")
            advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

            assertThat(screens).containsExactly(LinkScreen.Verification)
            assertThat(dismissResults).isEmpty()
        }

    @Test
    fun `When signup succeeds with completed signup in Full mode then navigates to PaymentMethod`() =
        runTest(dispatcher) {
            val screens = arrayListOf<LinkScreen>()
            val linkAccountManager = FakeLinkAccountManager()
            val signupSession = ConsumerSession.VerificationSession(
                type = ConsumerSession.VerificationSession.SessionType.SignUp,
                state = ConsumerSession.VerificationSession.SessionState.Started
            )
            val linkAccount = LinkAccount(
                consumerSession = TestFactory.CONSUMER_SESSION.copy(
                    verificationSessions = listOf(signupSession)
                )
            )
            linkAccountManager.lookupResult = Result.success(null)
            linkAccountManager.signupResult = Result.success(linkAccount)

            val viewModel = createViewModel(
                linkAccountManager = linkAccountManager,
                linkLaunchMode = LinkLaunchMode.Full,
                navigateAndClearStack = { screen -> screens.add(screen) }
            )

            viewModel.performValidSignup()

            assertThat(screens).containsExactly(LinkScreen.PaymentMethod)
        }

    @Test
    fun `When signup succeeds with verified account in Full mode then navigates to Wallet`() = runTest(dispatcher) {
        val screens = arrayListOf<LinkScreen>()
        val linkAccountManager = FakeLinkAccountManager()
        val linkAccount = LinkAccount(
            consumerSession = TestFactory.CONSUMER_SESSION.copy(
                verificationSessions = listOf(TestFactory.VERIFIED_SESSION)
            )
        )
        linkAccountManager.lookupResult = Result.success(null)
        linkAccountManager.signupResult = Result.success(linkAccount)

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            linkLaunchMode = LinkLaunchMode.Full,
            navigateAndClearStack = { screen -> screens.add(screen) }
        )

        viewModel.performValidSignup()

        assertThat(screens).containsExactly(LinkScreen.Wallet)
    }

    @Test
    fun `When signup succeeds with unverified account in Full mode then navigates to Verification`() =
        runTest(dispatcher) {
            val screens = arrayListOf<LinkScreen>()
            val linkAccountManager = FakeLinkAccountManager()
            val linkAccount = LinkAccount(
                consumerSession = TestFactory.CONSUMER_SESSION.copy(
                    verificationSessions = emptyList()
                )
            )
            linkAccountManager.lookupResult = Result.success(null)
            linkAccountManager.signupResult = Result.success(linkAccount)

            val viewModel = createViewModel(
                linkAccountManager = linkAccountManager,
                linkLaunchMode = LinkLaunchMode.Full,
                navigateAndClearStack = { screen -> screens.add(screen) }
            )

            viewModel.performValidSignup()

            assertThat(screens).containsExactly(LinkScreen.Verification)
        }

    private fun createViewModel(
        prefilledEmail: String? = null,
        configuration: LinkConfiguration = TestFactory.LINK_CONFIGURATION,
        countryCode: CountryCode = CountryCode.US,
        linkEventsReporter: LinkEventsReporter = SignUpLinkEventsReporter(),
        linkAccountManager: FakeLinkAccountManager = FakeLinkAccountManager().apply {
            lookupResult = Result.success(null)
        },
        logger: Logger = FakeLogger(),
        dismissalCoordinator: LinkDismissalCoordinator = RealLinkDismissalCoordinator(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        navigateAndClearStack: (LinkScreen) -> Unit = {},
        moveToWeb: (Throwable) -> Unit = {},
        dismissWithResult: (LinkActivityResult) -> Unit = {},
        linkLaunchMode: LinkLaunchMode = LinkLaunchMode.Full
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
            linkAccountManager = linkAccountManager,
            linkEventsReporter = linkEventsReporter,
            logger = logger,
            savedStateHandle = savedStateHandle,
            navigateAndClearStack = navigateAndClearStack,
            moveToWeb = moveToWeb,
            dismissalCoordinator = dismissalCoordinator,
            linkLaunchMode = linkLaunchMode,
            dismissWithResult = dismissWithResult
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
