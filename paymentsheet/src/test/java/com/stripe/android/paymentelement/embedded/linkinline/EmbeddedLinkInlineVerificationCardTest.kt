package com.stripe.android.paymentelement.embedded.linkinline

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineCardState.Action
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineCardState.Actions
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineCardState.MenuItem
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle
import com.stripe.android.paymentsheet.R
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.OTPElementFactory
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EmbeddedLinkInlineVerificationCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `card shows the email, title, recipient and Link logo`() = runScenario {
        composeRule.onNodeWithText(EMAIL).assertIsDisplayed()
        composeRule.onNodeWithText("Use your saved information").assert(isHeading())
        composeRule.onNodeWithText("Enter the code sent to \u2068$RECIPIENT\u2069.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Link").assertIsDisplayed()
    }

    @Test
    fun `tapping the code reports a code tap`() = runScenario {
        composeRule.onNodeWithTag(EMBEDDED_LINK_INLINE_CARD_CODE_TEST_TAG).performClick()

        codeTapCalls.awaitItem()
    }

    @Test
    fun `code can't be tapped while it's being sent`() = runScenario(
        state = cardState(isCodeEntryEnabled = false),
    ) {
        composeRule.onNodeWithTag(EMBEDDED_LINK_INLINE_CARD_CODE_TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun `close button closes the card`() = runScenario {
        composeRule.onNodeWithContentDescription("Close").performClick()

        closeCalls.awaitItem()
    }

    @Test
    fun `email opens a menu to switch accounts`() = runScenario {
        composeRule.onNodeWithTag(EMBEDDED_LINK_INLINE_CARD_EMAIL_TEST_TAG).performClick()
        composeRule.onNodeWithTag(EMBEDDED_LINK_INLINE_CARD_SWITCH_ACCOUNTS_TEST_TAG)
            .assertTextEquals("Switch accounts")
            .performClick()

        switchAccountCalls.awaitItem()
    }

    @Test
    fun `resend is shown inline when it's the only action`() = runScenario {
        composeRule.onNodeWithTag(EMBEDDED_LINK_INLINE_CARD_ACTION_TEST_TAG)
            .assertTextEquals("Resend code")
            .performClick()

        assertThat(actionCalls.awaitItem()).isEqualTo(Action.Resend)
    }

    @Test
    fun `resend can't be tapped during its cooldown`() = runScenario(
        state = cardState(
            actions = Actions.Resend(
                label = resolvableString(R.string.stripe_link_auth_resend_countdown, 7),
                isEnabled = false,
            ),
        ),
    ) {
        composeRule.onNodeWithTag(EMBEDDED_LINK_INLINE_CARD_ACTION_TEST_TAG)
            .assertTextEquals("Resend code in 7 seconds")
            .assertIsNotEnabled()
    }

    @Test
    fun `more options opens the menu of actions`() = runScenario(
        state = cardState(actions = MENU),
    ) {
        composeRule.onNodeWithTag(EMBEDDED_LINK_INLINE_CARD_ACTION_TEST_TAG)
            .assertTextEquals("More options")
            .performClick()
        composeRule.onNodeWithTag(EMBEDDED_LINK_INLINE_CARD_MENU_EMAIL_TEST_TAG).performClick()

        assertThat(actionCalls.awaitItem()).isEqualTo(Action.Email)
    }

    @Test
    fun `menu offers resend`() = runScenario(
        state = cardState(actions = MENU),
    ) {
        composeRule.onNodeWithTag(EMBEDDED_LINK_INLINE_CARD_ACTION_TEST_TAG).performClick()
        composeRule.onNodeWithTag(EMBEDDED_LINK_INLINE_CARD_MENU_RESEND_TEST_TAG).performClick()

        assertThat(actionCalls.awaitItem()).isEqualTo(Action.Resend)
    }

    @Test
    fun `actions can't be tapped while a code is being sent`() = runScenario(
        state = cardState(actions = MENU, isActionLoading = true),
    ) {
        composeRule.onNodeWithTag(EMBEDDED_LINK_INLINE_CARD_ACTION_TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun `no action button without actions`() = runScenario(
        state = cardState(actions = Actions.None),
    ) {
        composeRule.onNodeWithTag(EMBEDDED_LINK_INLINE_CARD_ACTION_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `error is shown below the code`() = runScenario(
        state = cardState(errorMessage = "That code isn't right."),
    ) {
        composeRule.onNodeWithTag(EMBEDDED_LINK_INLINE_CARD_ERROR_TEST_TAG)
            .assertTextEquals("That code isn't right.")
    }

    @Test
    fun `no error is shown without one`() = runScenario {
        composeRule.onNodeWithTag(EMBEDDED_LINK_INLINE_CARD_ERROR_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `code doesn't take focus until it's typed in place`() = runScenario {
        composeRule.onNodeWithTag(FIRST_DIGIT_TEST_TAG).assertIsNotFocused()
        composeRule.onNodeWithTag(EMBEDDED_LINK_INLINE_CARD_CODE_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `typing in place focuses the code and moves the tap target aside`() = runScenario(isTyping = true) {
        composeRule.onNodeWithTag(FIRST_DIGIT_TEST_TAG).assertIsFocused()
        composeRule.onNodeWithTag(EMBEDDED_LINK_INLINE_CARD_CODE_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `Done on the keyboard ends typing`() = runScenario(isTyping = true) {
        composeRule.onNodeWithTag(FIRST_DIGIT_TEST_TAG).performImeAction()

        doneCalls.awaitItem()
    }

    @Test
    fun `ending typing clears the code's focus`() = runScenario(isTyping = true) {
        composeRule.onNodeWithTag(FIRST_DIGIT_TEST_TAG).assertIsFocused()

        isTyping.value = false

        composeRule.onNodeWithTag(FIRST_DIGIT_TEST_TAG).assertIsNotFocused()
        composeRule.onNodeWithTag(EMBEDDED_LINK_INLINE_CARD_CODE_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `host clearing focus interrupts typing`() = runScenario(isTyping = true) {
        composeRule.onNodeWithTag(FIRST_DIGIT_TEST_TAG).assertIsFocused()

        composeRule.runOnIdle { requireNotNull(focusManager.value).clearFocus() }

        typingInterruptedCalls.awaitItem()
    }

    @Test
    fun `card leaving the screen interrupts typing`() = runScenario(isTyping = true) {
        isShown.value = false

        composeRule.onNodeWithTag(EMBEDDED_LINK_INLINE_CARD_TEST_TAG).assertDoesNotExist()
        typingInterruptedCalls.awaitItem()
    }

    private fun runScenario(
        state: EmbeddedLinkInlineCardState = cardState(),
        isTyping: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val codeTapCalls = Turbine<Unit>()
        val doneCalls = Turbine<Unit>()
        val typingInterruptedCalls = Turbine<Unit>()
        val closeCalls = Turbine<Unit>()
        val switchAccountCalls = Turbine<Unit>()
        val actionCalls = Turbine<Action>()
        val isTypingState = mutableStateOf(isTyping)
        val isShown = mutableStateOf(true)
        val focusManager = mutableStateOf<FocusManager?>(null)

        composeRule.setContent {
            focusManager.value = LocalFocusManager.current
            StripeTheme {
                if (isShown.value) {
                    EmbeddedLinkInlineVerificationCard(
                        state = state,
                        otpElement = OTPElementFactory.create(),
                        linkBrand = LinkBrand.Link,
                        rowStyle = RowStyle.FlatWithRadio.default,
                        isTyping = isTypingState.value,
                        focusRegionModifier = Modifier,
                        onCodeTapped = { codeTapCalls.add(Unit) },
                        onDone = { doneCalls.add(Unit) },
                        onTypingInterrupted = {
                            typingInterruptedCalls.add(Unit)
                            isTypingState.value = false
                        },
                        onClose = { closeCalls.add(Unit) },
                        onSwitchAccount = { switchAccountCalls.add(Unit) },
                        onAction = { actionCalls.add(it) },
                    )
                }
            }
        }

        Scenario(
            codeTapCalls = codeTapCalls,
            doneCalls = doneCalls,
            typingInterruptedCalls = typingInterruptedCalls,
            closeCalls = closeCalls,
            switchAccountCalls = switchAccountCalls,
            actionCalls = actionCalls,
            isTyping = isTypingState,
            isShown = isShown,
            focusManager = focusManager,
        ).block()

        codeTapCalls.ensureAllEventsConsumed()
        doneCalls.ensureAllEventsConsumed()
        typingInterruptedCalls.ensureAllEventsConsumed()
        closeCalls.ensureAllEventsConsumed()
        switchAccountCalls.ensureAllEventsConsumed()
        actionCalls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val codeTapCalls: Turbine<Unit>,
        val doneCalls: Turbine<Unit>,
        val typingInterruptedCalls: Turbine<Unit>,
        val closeCalls: Turbine<Unit>,
        val switchAccountCalls: Turbine<Unit>,
        val actionCalls: Turbine<Action>,
        val isTyping: MutableState<Boolean>,
        val isShown: MutableState<Boolean>,
        val focusManager: State<FocusManager?>,
    )

    private companion object {
        const val EMAIL = "jenny.rosen@example.com"
        const val FIRST_DIGIT_TEST_TAG = "OTP-0"
        const val RECIPIENT = "(•••) ••• ••55"

        val MENU = Actions.Menu(
            items = listOf(
                MenuItem(
                    action = Action.Resend,
                    label = resolvableString(R.string.stripe_verification_resend),
                    isEnabled = true,
                ),
                MenuItem(
                    action = Action.Email,
                    label = resolvableString(R.string.stripe_link_auth_email_code),
                    isEnabled = true,
                ),
            ),
            isEnabled = true,
        )

        fun cardState(
            isCodeEntryEnabled: Boolean = true,
            errorMessage: String? = null,
            actions: Actions = Actions.Resend(
                label = resolvableString(R.string.stripe_verification_resend),
                isEnabled = true,
            ),
            isActionLoading: Boolean = false,
        ) = EmbeddedLinkInlineCardState(
            email = EMAIL,
            recipient = RECIPIENT,
            isCodeEntryEnabled = isCodeEntryEnabled,
            errorMessage = errorMessage?.resolvableString,
            actions = actions,
            isActionLoading = isActionLoading,
            inputRevision = 0,
            invalidCodeRevision = 0,
        )
    }
}
