package com.stripe.android.link.ui.signup

import androidx.activity.ComponentActivity
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeLogger
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
        val viewModel = viewModel()

        composeTestRule.setContent {
            SignUpScreen(viewModel = viewModel)
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
            SignUpScreen(viewModel = viewModel)
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
            SignUpScreen(viewModel = viewModel)
        }

        dispatcher.scheduler.advanceTimeBy(1001)

        onEmailField().assertIsDisplayed()
        onSignUpButton().assertExists().assertIsNotEnabled()
        onPhoneField().assertIsDisplayed()
        onNameField().assertIsDisplayed()
    }

    @Test
    fun `field displayed, sign up enabled and error displayed when controllers are filled and sign up fails`() =
        runTest(dispatcher) {
            val linkAccountManager = FakeLinkAccountManager()
            val viewModel = viewModel(linkAccountManager = linkAccountManager)

            composeTestRule.setContent {
                SignUpScreen(viewModel)
            }

            linkAccountManager.signUpResult = Result.failure(Throwable("oops"))
            viewModel.onSignUpClick()

            dispatcher.scheduler.advanceTimeBy(1001)
            onErrorSection().assertExists().assert(hasAnyChild(hasText("oops")))
        }

    private fun viewModel(
        linkAccountManager: FakeLinkAccountManager = FakeLinkAccountManager(),
        customerInfo: LinkConfiguration.CustomerInfo = TestFactory.LINK_CUSTOMER_INFO
    ): SignUpViewModel {
        return SignUpViewModel(
            configuration = TestFactory.LINK_CONFIGURATION.copy(
                customerInfo = customerInfo
            ),
            linkAccountManager = linkAccountManager,
            linkEventsReporter = linkEventsReporter,
            logger = logger,
            navigate = {},
            navigateAndClearStack = {}
        )
    }

    private fun onEmailField() = composeTestRule.onNodeWithText("Email")
    private fun onPhoneField() = composeTestRule.onNodeWithText("Phone number")
    private fun onNameField() = composeTestRule.onNodeWithText("Full name")
    private fun onSignUpButton() = composeTestRule.onNodeWithText("Join Link")
    private fun onErrorSection() = composeTestRule.onNodeWithTag(SIGN_UP_ERROR_TAG)
}
