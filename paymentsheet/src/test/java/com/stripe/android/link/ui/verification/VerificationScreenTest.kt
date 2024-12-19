package com.stripe.android.link.ui.verification

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.core.Logger
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.testing.FakeLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class VerificationScreenTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @Test
    fun `title, email and otp should be displayed on screen load`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            VerificationScreen(viewModel)
        }

        onTitleField().assertIsDisplayed()
        onSubtitleTag().assertIsDisplayed()
        onOtpTag().assertIsDisplayed()
        onEmailTag().assertIsDisplayed().assertHasClickAction().assertIsEnabled()
        onErrorTag().assertDoesNotExist()
        onLoaderTag().assertDoesNotExist()
        onResendCodeButtonTag().assertIsDisplayed().assertHasClickAction().assertIsEnabled()
    }

    @Test
    fun `title, email, otp and error should be displayed when startVerification fails`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.startVerificationResult = Result.failure(RuntimeException(Throwable("oops")))

        val viewModel = createViewModel(linkAccountManager)

        composeTestRule.setContent {
            VerificationScreen(viewModel)
        }

        onTitleField().assertIsDisplayed()
        onSubtitleTag().assertIsDisplayed()
        onOtpTag().assertIsDisplayed()
        onEmailTag().assertIsDisplayed().assertHasClickAction().assertIsEnabled()
        onErrorTag().assertIsDisplayed()
        onLoaderTag().assertDoesNotExist()
        onResendCodeButtonTag().assertIsDisplayed().assertHasClickAction().assertIsEnabled()
    }

    @Test
    fun `title, email, otp and error should be displayed when confirmVerification fails`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.confirmVerificationResult = Result.failure(RuntimeException(Throwable("oops")))

        val viewModel = createViewModel(linkAccountManager)

        composeTestRule.setContent {
            VerificationScreen(viewModel)
        }

        viewModel.onVerificationCodeEntered("code")

        onTitleField().assertIsDisplayed()
        onSubtitleTag().assertIsDisplayed()
        onOtpTag().assertIsDisplayed()
        onEmailTag().assertIsDisplayed().assertHasClickAction().assertIsEnabled()
        onErrorTag().assertIsDisplayed()
        onLoaderTag().assertDoesNotExist()
        onResendCodeButtonTag().assertIsDisplayed().assertHasClickAction().assertIsEnabled()
    }

    @Test
    fun `title, email, otp and loader should be displayed when resending code`() = runTest(dispatcher) {
        val linkAccountManager = object : FakeLinkAccountManager() {
            override suspend fun startVerification(): Result<LinkAccount> {
                delay(1500)
                return super.startVerification()
            }
        }

        val viewModel = createViewModel(linkAccountManager)

        composeTestRule.setContent {
            VerificationScreen(viewModel)
        }

        viewModel.resendCode()

        onTitleField().assertIsDisplayed()
        onSubtitleTag().assertIsDisplayed()
        onOtpTag().assertIsDisplayed()
        onEmailTag().assertIsDisplayed().assertIsEnabled()
        onErrorTag().assertDoesNotExist()
        onLoaderTag().assertIsDisplayed()
        onResendCodeButtonTag().assertIsDisplayed().assertHasClickAction().assertIsNotEnabled()
    }

    @Test
    fun `title, email, otp should be displayed when verification is in process`() = runTest(dispatcher) {
        val linkAccountManager = object : FakeLinkAccountManager() {
            override suspend fun confirmVerification(code: String): Result<LinkAccount> {
                delay(5500)
                return super.confirmVerification(code)
            }
        }

        val viewModel = createViewModel(linkAccountManager)

        composeTestRule.setContent {
            VerificationScreen(viewModel)
        }

        viewModel.otpElement.controller.onAutofillDigit("555555")

        onTitleField().assertIsDisplayed()
        onSubtitleTag().assertIsDisplayed()
        onOtpTag().assertIsDisplayed()
        onEmailTag().assertIsDisplayed().assertHasClickAction().assertIsNotEnabled()
        onErrorTag().assertDoesNotExist()
        onLoaderTag().assertDoesNotExist()
        onResendCodeButtonTag().assertIsDisplayed().assertHasClickAction().assertIsNotEnabled()
    }

    private fun createViewModel(
        linkAccountManager: LinkAccountManager = FakeLinkAccountManager(),
        linkEventsReporter: LinkEventsReporter = FakeLinkEventsReporter(),
        logger: Logger = FakeLogger(),
    ): VerificationViewModel {
        return VerificationViewModel(
            linkAccountManager = linkAccountManager,
            linkEventsReporter = linkEventsReporter,
            logger = logger,
            navigateAndClearStack = {},
            goBack = {},
            linkAccount = TestFactory.LINK_ACCOUNT
        )
    }

    private fun onTitleField() = composeTestRule.onNodeWithTag(VERIFICATION_TITLE_TAG)
    private fun onSubtitleTag() = composeTestRule.onNodeWithTag(VERIFICATION_SUBTITLE_TAG)
    private fun onOtpTag() = composeTestRule.onNodeWithTag(VERIFICATION_OTP_TAG)
    private fun onEmailTag() = composeTestRule.onNodeWithTag(VERIFICATION_CHANGE_EMAIL_TAG)
    private fun onErrorTag() = composeTestRule.onNodeWithTag(VERIFICATION_ERROR_TAG)
    private fun onLoaderTag() = composeTestRule.onNodeWithTag(VERIFICATION_RESEND_LOADER_TAG)
    private fun onResendCodeButtonTag() = composeTestRule.onNodeWithTag(VERIFICATION_RESEND_CODE_BUTTON_TAG)
}
