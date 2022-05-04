package com.stripe.android.link.ui.verification

import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.KEYCODE_DEL
import androidx.activity.ComponentActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.asLiveData
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.R
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.OTPController
import com.stripe.android.ui.core.elements.OTPElement
import com.stripe.android.ui.core.elements.OTPSpec
import com.stripe.android.ui.core.forms.FormFieldEntry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalAnimationApi
@RunWith(AndroidJUnit4::class)
internal class VerificationScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun when_isProcessing_then_views_are_disabled() {
        setContent(isProcessing = true)

        onResendCodeButton().assertIsNotEnabled()
    }

    @Test
    fun change_email_button_triggers_action() {
        var count = 0
        setContent(
            onChangeEmailClick = {
                count++
            }
        )

        onChangeEmailButton().performClick()

        assertThat(count).isEqualTo(1)
    }

    @Test
    fun resend_code_button_triggers_action() {
        var count = 0
        setContent(
            onResendCodeClick = {
                count++
            }
        )

        onResendCodeButton().performClick()

        assertThat(count).isEqualTo(1)
    }

    @Test
    fun buttons_do_not_trigger_event_when_disabled() {
        var count = 0
        setContent(
            isProcessing = true,
            onChangeEmailClick = {
                count++
            },
            onResendCodeClick = {
                count++
            }
        )

        onResendCodeButton().performClick()
        onChangeEmailButton().performClick()

        assertThat(count).isEqualTo(0)
    }

    @Test
    fun entering_valid_otp_emits_valid_value() {
        val otpElement = OTPElement(IdentifierSpec.Generic("otp"), OTPController(3))
        setContent(otpElement = otpElement)

        var otpValue: FormFieldEntry? = null
        otpElement.getFormFieldValueFlow().asLiveData().observeForever {
            otpValue = it.firstOrNull()?.second
        }

        onOtpField(0).performTextInput("1")
        onOtpField(1).performTextInput("2")
        onOtpField(2).performTextInput("3")

        assertThat(otpValue?.isComplete).isTrue()
        assertThat(otpValue?.value).isEqualTo("123")
    }

    @Test
    fun entering_valid_otp_at_once_emits_valid_value() {
        val otpElement = OTPElement(IdentifierSpec.Generic("otp"), OTPController(6))
        setContent(otpElement = otpElement)

        var otpValue: FormFieldEntry? = null
        otpElement.getFormFieldValueFlow().asLiveData().observeForever {
            otpValue = it.firstOrNull()?.second
        }

        onOtpField(0).performTextInput("123456")

        assertThat(otpValue?.isComplete).isTrue()
        assertThat(otpValue?.value).isEqualTo("123456")
    }

    @Test
    fun otp_field_on_delete_deletes_value_and_moves_focus() {
        val otpElement = OTPElement(IdentifierSpec.Generic("otp"), OTPController(6))
        setContent(otpElement = otpElement)

        var otpValue: FormFieldEntry? = null
        otpElement.getFormFieldValueFlow().asLiveData().observeForever {
            otpValue = it.firstOrNull()?.second
        }

        onOtpField(0).performTextInput("12")

        assertThat(otpValue?.isComplete).isFalse()
        assertThat(otpValue?.value).isEqualTo("12")

        onOtpField(2).performKeyPress(KeyEvent(NativeKeyEvent(ACTION_DOWN, KEYCODE_DEL)))

        assertThat(otpValue?.isComplete).isFalse()
        assertThat(otpValue?.value).isEqualTo("1")

        onOtpField(1).assertIsFocused()
            .performKeyPress(KeyEvent(NativeKeyEvent(ACTION_DOWN, KEYCODE_DEL)))

        assertThat(otpValue?.isComplete).isFalse()
        assertThat(otpValue?.value).isEqualTo("")

        onOtpField(0).assertIsFocused()
            .performKeyPress(KeyEvent(NativeKeyEvent(ACTION_DOWN, KEYCODE_DEL)))

        assertThat(otpValue?.isComplete).isFalse()
        assertThat(otpValue?.value).isEqualTo("")
    }

    @Test
    fun focus_on_first_otp_field_at_start() {
        setContent()
        onOtpField(0).assertIsFocused()
    }

    private fun setContent(
        redactedPhoneNumber: String = "+1********23",
        email: String = "test@stripe.com",
        otpElement: OTPElement = OTPSpec.transform(),
        isProcessing: Boolean = false,
        onBack: () -> Unit = { },
        onChangeEmailClick: () -> Unit = { },
        onResendCodeClick: () -> Unit = { }
    ) = composeTestRule.setContent {
        DefaultLinkTheme {
            VerificationBody(
                headerStringResId = R.string.verification_header,
                messageStringResId = R.string.verification_message,
                showChangeEmailMessage = true,
                redactedPhoneNumber = redactedPhoneNumber,
                email = email,
                otpElement = otpElement,
                isProcessing = isProcessing,
                onBack = onBack,
                onChangeEmailClick = onChangeEmailClick,
                onResendCodeClick = onResendCodeClick
            )
        }
    }

    private fun onResendCodeButton() = composeTestRule.onNodeWithText("Resend code")
    private fun onChangeEmailButton() = composeTestRule.onNodeWithText("Change email")
    private fun onOtpField(index: Int) = composeTestRule.onNodeWithTag("OTP-$index")
}
