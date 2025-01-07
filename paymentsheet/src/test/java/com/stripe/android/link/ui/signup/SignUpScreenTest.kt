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
import androidx.navigation.NavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.testing.FakeLogger
import com.stripe.android.testing.PaymentIntentFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
internal class SignUpScreenTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val linkConfiguration = LinkConfiguration(
        stripeIntent = PaymentIntentFactory.create(),
        customerInfo = LinkConfiguration.CustomerInfo(null, null, null, null),
        flags = mapOf(),
        merchantName = "Test merchant inc.",
        merchantCountryCode = "US",
        passthroughModeEnabled = false,
        cardBrandChoice = null,
        shippingDetails = null,
    )
    private val linkAccountManager = FakeLinkAccountManager()
    private val linkEventsReporter = object : FakeLinkEventsReporter() {
        override fun onSignupFlowPresented() = Unit
        override fun onSignupFailure(isInline: Boolean, error: Throwable) = Unit
    }
    private val logger = FakeLogger()

    private lateinit var viewModel: SignUpViewModel
    private val navController: NavHostController = mock()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @Test
    fun `only email field displayed when controllers are empty`() = runTest(dispatcher) {
        viewModel = SignUpViewModel(
            configuration = linkConfiguration,
            linkAccountManager = linkAccountManager,
            linkEventsReporter = linkEventsReporter,
            logger = logger
        )

        composeTestRule.setContent {
            SignUpScreen(viewModel = viewModel, navController = navController)
        }

        onEmailField().assertIsDisplayed()
        onSignUpButton().assertDoesNotExist()
        onErrorSection().assertDoesNotExist()
        onPhoneField().assertDoesNotExist()
        onNameField().assertDoesNotExist()
    }

    @Test
    fun `all fields displayed and sign up enabled when all controllers are filled`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        viewModel = SignUpViewModel(
            configuration = linkConfiguration.copy(
                customerInfo = LinkConfiguration.CustomerInfo(
                    name = "jane doe",
                    email = "test@test.com",
                    phone = "+11234567891",
                    billingCountryCode = "CA"
                )
            ),
            linkAccountManager = linkAccountManager,
            linkEventsReporter = linkEventsReporter,
            logger = logger
        )

        composeTestRule.setContent {
            SignUpScreen(viewModel = viewModel, navController = navController)
        }

        dispatcher.scheduler.advanceTimeBy(1500)

        onEmailField().assertIsDisplayed()
        onSignUpButton().assertExists().assertIsEnabled().assertHasClickAction()
        onPhoneField().assertIsDisplayed()
        onNameField().assertIsDisplayed()
    }

    @Test
    fun `all fields displayed when email controller is filled`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        viewModel = SignUpViewModel(
            configuration = linkConfiguration.copy(
                customerInfo = LinkConfiguration.CustomerInfo(
                    name = null,
                    email = "test@test.com",
                    phone = null,
                    billingCountryCode = null
                )
            ),
            linkAccountManager = linkAccountManager,
            linkEventsReporter = linkEventsReporter,
            logger = logger
        )

        composeTestRule.setContent {
            SignUpScreen(viewModel = viewModel, navController = navController)
        }

        dispatcher.scheduler.advanceTimeBy(1000)

        onEmailField().assertIsDisplayed()
        onSignUpButton().assertExists().assertIsNotEnabled()
        onPhoneField().assertIsDisplayed()
        onNameField().assertIsDisplayed()
    }

    @Test
    fun `field displayed, sign up enabled and error displayed when controllers are filled and sign up fails`() =
        runTest(dispatcher) {
            val linkAccountManager = FakeLinkAccountManager()
            viewModel = SignUpViewModel(
                configuration = linkConfiguration.copy(
                    customerInfo = LinkConfiguration.CustomerInfo(
                        name = "jane doe",
                        email = "test@test.com",
                        phone = "+11234567891",
                        billingCountryCode = "CA"
                    )
                ),
                linkAccountManager = linkAccountManager,
                linkEventsReporter = linkEventsReporter,
                logger = logger
            )

            composeTestRule.setContent {
                SignUpScreen(viewModel = viewModel, navController = navController)
            }

            linkAccountManager.signUpResult = Result.failure(Throwable("oops"))
            viewModel.onSignUpClick()

            dispatcher.scheduler.advanceTimeBy(1000)

            onErrorSection().assertExists().assert(hasAnyChild(hasText("oops")))
        }

    private fun onEmailField() = composeTestRule.onNodeWithText("Email")
    private fun onPhoneField() = composeTestRule.onNodeWithText("Phone number")
    private fun onNameField() = composeTestRule.onNodeWithText("Full name")
    private fun onSignUpButton() = composeTestRule.onNodeWithText("Join Link")
    private fun onErrorSection() = composeTestRule.onNodeWithTag(SIGN_UP_ERROR_TAG)
}
