package com.stripe.android.uicore.elements

import android.os.Build
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import com.stripe.android.testing.createComposeCleanupRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class OTPElementUITest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    fun `single digit advances focus`() {
        val element = testElement()

        composeTestRule.setContent {
            OTPElementUI(
                enabled = true,
                element = element,
            )
        }

        otpNode(0).requestFocus()
        otpNode(0).performTextInput("1")

        otpNode(1).assertIsFocused()
    }

    @Test
    fun `pasting full otp jumps focus to last box`() {
        val element = testElement()

        composeTestRule.setContent {
            OTPElementUI(
                enabled = true,
                element = element,
            )
        }

        otpNode(0).requestFocus()
        otpNode(0).performTextReplacement("123456")

        otpNode(5).assertIsFocused()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `backspace on empty box retreats focus`() {
        val element = testElement()

        composeTestRule.setContent {
            OTPElementUI(
                enabled = true,
                element = element,
            )
        }

        otpNode(0).requestFocus()
        otpNode(0).performTextInput("1")

        otpNode(1).assertIsFocused()
        otpNode(1).performKeyInput {
            pressKey(Key.Backspace)
        }

        otpNode(0).assertIsFocused()
    }

    @Test
    fun `tapping any box focuses the next digit`() {
        val element = testElement()

        composeTestRule.setContent {
            OTPElementUI(
                enabled = true,
                element = element,
            )
        }

        otpNode(4).performClick()
        otpNode(0).assertIsFocused()

        otpNode(0).performTextInput("1")
        otpNode(3).performClick()
        otpNode(1).assertIsFocused()
    }

    @Test
    fun `focus requester focuses the next digit`() {
        val element = testElement()
        val focusRequester = FocusRequester()

        composeTestRule.setContent {
            OTPElementUI(
                enabled = true,
                element = element,
                focusRequester = focusRequester,
            )
        }

        element.controller.onValueChanged(0, "12")
        composeTestRule.runOnIdle { focusRequester.requestFocus() }

        otpNode(2).assertIsFocused()
    }

    @Test
    fun `clearing the code moves focus back to the first box`() {
        val element = testElement()

        composeTestRule.setContent {
            OTPElementUI(
                enabled = true,
                element = element,
            )
        }

        otpNode(0).requestFocus()
        otpNode(0).performTextInput("123")
        otpNode(3).assertIsFocused()

        element.controller.reset()

        otpNode(0).assertIsFocused()
    }

    private fun otpNode(index: Int): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithTag("OTP-$index")
    }

    private fun testElement(): OTPElement {
        return OTPElement(
            identifier = FormFieldId.Generic("otp"),
            controller = OTPController(),
        )
    }
}

private fun SemanticsNodeInteraction.requestFocus() {
    performSemanticsAction(SemanticsActions.RequestFocus)
}
