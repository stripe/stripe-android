package com.stripe.android.link.ui.verification

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.PrimaryButtonTag
import com.stripe.android.link.ui.verification.LinkAuthFlowState.Action
import com.stripe.android.link.ui.verification.LinkAuthFlowState.Screen
import com.stripe.android.model.LinkBrand
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.elements.OTPElementFactory
import com.stripe.android.uicore.elements.PhoneNumberController
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class VerificationBodyAuthFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `email recipient is shown in the subtitle`() = runScenario(
        authFlow = authFlowState(recipient = EMAIL, actions = listOf(Action.Resend)),
    ) {
        composeRule.onNodeWithTag(VERIFICATION_SUBTITLE_TAG)
            .assert(hasText(EMAIL, substring = true))
        composeRule.onNodeWithTag(VERIFICATION_RESEND_CODE_BUTTON_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(VERIFICATION_MORE_OPTIONS_TAG).assertDoesNotExist()
    }

    @Test
    fun `more options menu offers resend and email code`() = runScenario(
        authFlow = authFlowState(actions = listOf(Action.Resend, Action.Email)),
    ) {
        composeRule.onNodeWithTag(VERIFICATION_RESEND_CODE_BUTTON_TAG).assertDoesNotExist()

        composeRule.onNodeWithTag(VERIFICATION_MORE_OPTIONS_TAG).performClick()
        composeRule.onNodeWithTag(VERIFICATION_MENU_EMAIL_CODE_TAG).performClick()

        assertThat(emailCodeClicks.awaitItem()).isEqualTo(Unit)

        composeRule.onNodeWithTag(VERIFICATION_MORE_OPTIONS_TAG).performClick()
        composeRule.onNodeWithTag(VERIFICATION_MENU_RESEND_TAG).performClick()

        assertThat(resendClicks.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `resend cooldown is shown instead of the resend button`() = runScenario(
        authFlow = authFlowState(
            actions = listOf(Action.Resend),
            canResend = false,
            resendSecondsRemaining = 7,
        ),
    ) {
        composeRule.onNodeWithTag(VERIFICATION_RESEND_COUNTDOWN_TAG)
            .assertIsDisplayed()
            .assert(hasText("7", substring = true))
        composeRule.onNodeWithTag(VERIFICATION_RESEND_CODE_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `code entry is disabled when the challenge cannot accept codes`() = runScenario(
        authFlow = authFlowState(codeEntryEnabled = false),
    ) {
        repeat(OTP_LENGTH) { index ->
            composeRule.onNodeWithTag("OTP-$index", useUnmergedTree = true).assertIsNotEnabled()
        }
    }

    @Test
    fun `dialog shows a back button when the flow can go back`() = runScenario(
        isDialog = true,
        authFlow = authFlowState(canGoBack = true),
    ) {
        composeRule.onNodeWithTag(VERIFICATION_HEADER_IMAGE_TAG).assertDoesNotExist()

        composeRule.onNodeWithTag(VERIFICATION_HEADER_BACK_BUTTON_TAG).performClick()

        assertThat(navigateBackClicks.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `phone match screen shows the digit hint and submits`() = runScenario(
        authFlow = authFlowState(screen = Screen.PhoneMatch),
        phoneNumber = "5555555542",
    ) {
        composeRule.onNodeWithTag(PHONE_MATCH_TITLE_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(VERIFICATION_OTP_TAG).assertDoesNotExist()
        composeRule.onNode(hasText("••42", substring = true)).assertExists()

        composeRule.onNodeWithTag(PHONE_MATCH_CONTINUE_TAG).performClick()

        assertThat(phoneNumberSubmissions.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `phone match continue is disabled until the number is complete`() = runScenario(
        authFlow = authFlowState(screen = Screen.PhoneMatch),
    ) {
        composeRule.onNodeWithTag(PrimaryButtonTag).assertIsNotEnabled()
    }

    @Test
    fun `phone match shows inline errors`() = runScenario(
        authFlow = authFlowState(screen = Screen.PhoneMatch),
        errorMessage = "Wrong number",
    ) {
        composeRule.onNode(hasText("Wrong number") and hasAnyAncestor(hasTestTag(PHONE_MATCH_ERROR_TAG)))
            .assertIsDisplayed()
    }

    @Test
    fun `blocked screen shows its message without code entry`() = runScenario(
        authFlow = authFlowState(screen = Screen.Blocked),
        errorMessage = "Blocked",
    ) {
        composeRule.onNodeWithTag(VERIFICATION_STATUS_TAG).assert(hasText("Blocked"))
        composeRule.onNodeWithTag(VERIFICATION_OTP_TAG).assertDoesNotExist()
    }

    private fun runScenario(
        authFlow: VerificationViewState.AuthFlowViewState,
        isDialog: Boolean = false,
        errorMessage: String? = null,
        phoneNumber: String = "",
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val scenario = Scenario(
            emailCodeClicks = Turbine(),
            resendClicks = Turbine(),
            navigateBackClicks = Turbine(),
            phoneNumberSubmissions = Turbine(),
        )
        val otpElement: OTPElement = OTPElementFactory.create()
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initialValue = phoneNumber,
            initiallySelectedCountryCode = "US",
        )
        val state = VerificationViewState(
            isProcessing = false,
            requestFocus = false,
            errorMessage = errorMessage?.resolvableString,
            isSendingNewCode = false,
            didSendNewCode = false,
            redactedPhoneNumber = "(•••) ••• ••42",
            email = EMAIL,
            isDialog = isDialog,
            allowLogout = true,
            defaultPayment = null,
            linkBrand = LinkBrand.Link,
            authFlow = authFlow,
        )

        composeRule.setContent {
            DefaultLinkTheme {
                VerificationBody(
                    state = state,
                    otpElement = otpElement,
                    phoneNumberController = phoneNumberController,
                    onBack = {},
                    onNavigateBack = { scenario.navigateBackClicks.add(Unit) },
                    onFocusRequested = {},
                    didShowCodeSentNotification = {},
                    onChangeEmailClick = {},
                    onResendCodeClick = { scenario.resendClicks.add(Unit) },
                    onEmailCodeClick = { scenario.emailCodeClicks.add(Unit) },
                    onPhoneNumberSubmitted = { scenario.phoneNumberSubmissions.add(Unit) },
                    onConsentShown = {},
                )
            }
        }

        scenario.block()

        scenario.emailCodeClicks.ensureAllEventsConsumed()
        scenario.resendClicks.ensureAllEventsConsumed()
        scenario.navigateBackClicks.ensureAllEventsConsumed()
        scenario.phoneNumberSubmissions.ensureAllEventsConsumed()
    }

    private class Scenario(
        val emailCodeClicks: Turbine<Unit>,
        val resendClicks: Turbine<Unit>,
        val navigateBackClicks: Turbine<Unit>,
        val phoneNumberSubmissions: Turbine<Unit>,
    )

    private fun authFlowState(
        screen: Screen = Screen.Otp,
        recipient: String = "(•••) ••• ••42",
        canGoBack: Boolean = false,
        codeEntryEnabled: Boolean = true,
        actions: List<Action> = listOf(Action.Resend),
        canResend: Boolean = true,
        resendSecondsRemaining: Int = 0,
    ) = VerificationViewState.AuthFlowViewState(
        screen = screen,
        recipient = recipient,
        canGoBack = canGoBack,
        codeEntryEnabled = codeEntryEnabled,
        actions = actions,
        canResend = canResend,
        isResending = false,
        resendSecondsRemaining = resendSecondsRemaining,
        phoneNumberLastTwoDigits = "42",
    )

    private companion object {
        const val EMAIL = "jane@example.com"
        const val OTP_LENGTH = 6
    }
}
