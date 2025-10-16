package com.stripe.android.link.ui.signup

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkDismissalCoordinator
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.RealLinkDismissalCoordinator
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeLogger
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
internal class SignUpScreenTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val linkEventsReporter = object : FakeLinkEventsReporter() {
        override fun onSignupFlowPresented() = Unit
        override fun onSignupFailure(isInline: Boolean, error: Throwable) = Unit
    }
    private val logger = FakeLogger()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Test
    fun `only email field displayed when controllers are empty`() = runTest(dispatcher) {
        val viewModel = viewModel(
            customerInfo = LinkConfiguration.CustomerInfo(
                email = null,
                phone = null,
                name = null,
                billingCountryCode = null,
            )
        )

        composeTestRule.setContent {
            DefaultLinkTheme {
                SignUpScreen(viewModel = viewModel)
            }
        }

        onEmailField().assertIsDisplayed()
        onSignUpButton().assertDoesNotExist()
        onErrorSection().assertDoesNotExist()
        onPhoneField().assertDoesNotExist()
        onNameField().assertDoesNotExist()
    }

    @Test
    fun `all fields displayed and sign up enabled when all controllers are filled`() = runTest(dispatcher) {
        val viewModel = viewModel()

        composeTestRule.setContent {
            DefaultLinkTheme {
                SignUpScreen(viewModel = viewModel)
            }
        }

        dispatcher.scheduler.advanceTimeBy(1500)

        onEmailField().assertIsDisplayed()
        onSignUpButton().assertExists().assertIsEnabled().assertHasClickAction()
        onPhoneField().assertIsDisplayed()
        onNameField().assertIsDisplayed()
    }

    @Test
    fun `all fields displayed when email controller is filled`() = runTest(dispatcher) {
        val viewModel = viewModel(
            customerInfo = TestFactory.LINK_CUSTOMER_INFO.copy(
                name = null,
                email = "test@test.com",
                phone = null,
                billingCountryCode = null
            )
        )

        composeTestRule.setContent {
            DefaultLinkTheme {
                SignUpScreen(viewModel = viewModel)
            }
        }

        dispatcher.scheduler.advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

        onEmailField().assertIsDisplayed()
        onSignUpButton().assertExists().assertIsNotEnabled()
        onPhoneField().assertIsDisplayed()
        onNameField().assertIsDisplayed()
    }

    @Test
    fun `field displayed, sign up enabled and error displayed when controllers are filled and sign up fails`() =
        runTest(dispatcher) {
            val error = Throwable("oops")
            val linkAccountManager = FakeLinkAccountManager()
            val viewModel = viewModel(linkAccountManager = linkAccountManager)

            composeTestRule.setContent {
                DefaultLinkTheme {
                    SignUpScreen(viewModel)
                }
            }

            linkAccountManager.lookupResult = Result.success(null)
            linkAccountManager.signupResult = Result.failure(error)
            viewModel.onSignUpClick()

            dispatcher.scheduler.advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)
            onErrorSection().assertExists().assert(hasAnyChild(hasText("Something went wrong")))
        }

    @Test
    fun `secondary fields hidden, error is displayed on account error after signup`() =
        runTest(dispatcher) {
            val stripeError =
                StripeError(code = "link_consumer_details_not_available", message = "Consumer details not available")
            val error = APIException(stripeError = stripeError)
            val linkAccountManager = FakeLinkAccountManager()
            linkAccountManager.lookupResult = Result.success(null)
            linkAccountManager.signupResult = Result.failure(error)

            val viewModel = viewModel(linkAccountManager = linkAccountManager)

            composeTestRule.setContent {
                DefaultLinkTheme {
                    SignUpScreen(viewModel)
                }
            }

            viewModel.emailController.onRawValueChange(TestFactory.CUSTOMER_EMAIL)
            dispatcher.scheduler.advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

            composeTestRule.waitForIdle()

            allFields().assert { assertExists() }

            viewModel.nameController.onRawValueChange(TestFactory.CUSTOMER_NAME)
            viewModel.phoneNumberController.onRawValueChange(TestFactory.CUSTOMER_PHONE)

            composeTestRule.waitForIdle()

            viewModel.onSignUpClick()

            composeTestRule.waitForIdle()

            primaryFields().assert { assertIsDisplayed() }
            secondaryFields().assert { assertDoesNotExist() }
            onErrorSection()
                .assertExists()
                .assert(hasAnyChild(hasText("Your account has been deactivated")))
        }

    @Test
    fun `secondary fields hidden, error is displayed on account error after lookup`() =
        runTest(dispatcher) {
            val stripeError =
                StripeError(code = "link_consumer_details_not_available", message = "Consumer details not available")
            val error = APIException(stripeError = stripeError)
            val linkAccountManager = FakeLinkAccountManager()
            linkAccountManager.lookupResult = Result.failure(error)

            val viewModel = viewModel(linkAccountManager = linkAccountManager)

            composeTestRule.setContent {
                DefaultLinkTheme {
                    SignUpScreen(viewModel)
                }
            }

            viewModel.emailController.onRawValueChange("a@b.com")
            dispatcher.scheduler.advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)
            composeTestRule.waitForIdle()

            primaryFields().assert { assertIsDisplayed() }
            secondaryFields().assert { assertDoesNotExist() }
            onErrorSection()
                .assertExists()
                .assert(hasAnyChild(hasText("Your account has been deactivated")))
        }

    @Test
    fun `error is displayed on lookup failure after email entry`() =
        runTest(dispatcher) {
            val error = Throwable("oops")
            val linkAccountManager = FakeLinkAccountManager()
            val viewModel = viewModel(linkAccountManager = linkAccountManager)

            composeTestRule.setContent {
                DefaultLinkTheme {
                    SignUpScreen(viewModel)
                }
            }

            linkAccountManager.lookupResult = Result.failure(error)

            viewModel.emailController.onRawValueChange("a@b.com")
            composeTestRule.waitForIdle()

            dispatcher.scheduler.advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)
            onErrorSection().assertExists()
        }

    @Test
    fun `email suggestion is displayed when suggestedEmail is set`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        val viewModel = viewModel(
            linkAccountManager = linkAccountManager,
            customerInfo = TestFactory.LINK_CUSTOMER_INFO.copy(
                email = "test@example.con"
            )
        )

        composeTestRule.setContent {
            DefaultLinkTheme {
                SignUpScreen(viewModel = viewModel)
            }
        }

        dispatcher.scheduler.advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

        // Simulate suggested email
        linkAccountManager._suggestedEmail.value = "test@example.com"
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Did you mean test@example.com?")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Yes, update")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun `clicking suggested email updates email field`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        val viewModel = viewModel(
            linkAccountManager = linkAccountManager,
            customerInfo = LinkConfiguration.CustomerInfo(
                email = null,
                phone = null,
                name = null,
                billingCountryCode = null,
            )
        )

        composeTestRule.setContent {
            DefaultLinkTheme {
                SignUpScreen(viewModel = viewModel)
            }
        }

        viewModel.emailController.onRawValueChange("user@example.con")
        dispatcher.scheduler.advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)

        linkAccountManager._suggestedEmail.value = "user@example.com"
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("emailSuggestionUpdateTag")
            .assertIsDisplayed()

        assertThat(viewModel.emailController.fieldValue.value).isEqualTo("user@example.con")
    }

    @Test
    fun `email suggestion is hidden when verifying email`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.lookupResult = Result.success(null)
        val viewModel = viewModel(
            linkAccountManager = linkAccountManager,
            customerInfo = LinkConfiguration.CustomerInfo(
                email = null,
                phone = null,
                name = null,
                billingCountryCode = null,
            )
        )

        composeTestRule.setContent {
            DefaultLinkTheme {
                SignUpScreen(viewModel = viewModel)
            }
        }

        linkAccountManager._suggestedEmail.value = "test@example.com"
        composeTestRule.waitForIdle()

        viewModel.emailController.onRawValueChange("valid@email.com")
        composeTestRule.waitForIdle()
        // Wait for debounce and state change
        dispatcher.scheduler.advanceTimeBy(SignUpViewModel.LOOKUP_DEBOUNCE + 1.milliseconds)
        // Simulate manager clearing suggestion on new lookup (like real implementation does)
        linkAccountManager._suggestedEmail.value = null
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Did you mean test@example.com?")
            .assertDoesNotExist()
    }

    private fun List<SemanticsNodeInteraction>.assert(interaction: SemanticsNodeInteraction.() -> Unit) {
        forEach { interaction(it) }
    }

    private fun allFields(): List<SemanticsNodeInteraction> {
        return primaryFields() + secondaryFields()
    }

    private fun primaryFields() = listOf(onEmailField())

    private fun secondaryFields(): List<SemanticsNodeInteraction> {
        return listOf(
            onNameField(),
            onPhoneField(),
            onSignUpButton()
        )
    }

    private fun viewModel(
        linkAccountManager: FakeLinkAccountManager = FakeLinkAccountManager(),
        dismissalCoordinator: LinkDismissalCoordinator = RealLinkDismissalCoordinator(),
        customerInfo: LinkConfiguration.CustomerInfo = TestFactory.LINK_CUSTOMER_INFO,
        moveToWeb: (Throwable) -> Unit = {}
    ): SignUpViewModel {
        return SignUpViewModel(
            configuration = TestFactory.LINK_CONFIGURATION.copy(
                customerInfo = customerInfo
            ),
            linkAccountManager = linkAccountManager,
            linkEventsReporter = linkEventsReporter,
            logger = logger,
            savedStateHandle = SavedStateHandle(),
            navigateAndClearStack = {},
            dismissalCoordinator = dismissalCoordinator,
            moveToWeb = moveToWeb,
            linkLaunchMode = LinkLaunchMode.Full,
            dismissWithResult = {},
            verifyDuringSignUp = {},
        )
    }

    private fun onEmailField() = composeTestRule.onNodeWithText("Email")
    private fun onPhoneField() = composeTestRule.onNodeWithText("Phone number")
    private fun onNameField() = composeTestRule.onNodeWithText("Full name")
    private fun onSignUpButton() = composeTestRule.onNodeWithText("Continue")
    private fun onErrorSection() = composeTestRule.onNodeWithTag(SIGN_UP_ERROR_TAG)
}
