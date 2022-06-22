package com.stripe.android.link.ui.signup

import androidx.activity.ComponentActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.progressIndicatorTestTag
import com.stripe.android.ui.core.elements.PhoneNumberController
import com.stripe.android.ui.core.elements.SimpleTextFieldController
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalAnimationApi
@RunWith(AndroidJUnit4::class)
internal class SignUpScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun status_inputting_email_shows_only_email_field() {
        setContent(SignUpState.InputtingEmail)

        onEmailField().assertExists()
        onEmailField().assertIsEnabled()
        onProgressIndicator().assertDoesNotExist()
        onPhoneField().assertDoesNotExist()
        onSignUpButton().assertDoesNotExist()
    }

    @Test
    fun status_verifying_email_is_disabled() {
        setContent(SignUpState.VerifyingEmail)

        onEmailField().assertExists()
        onEmailField().assertIsNotEnabled()
        onProgressIndicator().assertExists()
        onPhoneField().assertDoesNotExist()
        onSignUpButton().assertDoesNotExist()
    }

    @Test
    fun status_inputting_phone_shows_all_fields() {
        setContent(SignUpState.InputtingPhone)

        onEmailField().assertExists()
        onEmailField().assertIsEnabled()
        onProgressIndicator().assertDoesNotExist()
        onPhoneField().assertExists()
        onPhoneField().assertIsEnabled()
        onSignUpButton().assertExists()
    }

    @Test
    fun header_message_is_correct_before_collecting_email() {
        setContent(SignUpState.InputtingEmail)

        composeTestRule.onNodeWithText("Secure 1-click checkout").assertExists()
        composeTestRule.onNodeWithText("Save your info for secure 1-click checkout")
            .assertDoesNotExist()
    }

    @Test
    fun header_message_is_correct_when_collecting_phone_number() {
        setContent(SignUpState.InputtingPhone)

        composeTestRule.onNodeWithText("Secure 1-click checkout").assertDoesNotExist()
        composeTestRule.onNodeWithText("Save your info for secure 1-click checkout").assertExists()
    }

    @Test
    fun signup_button_is_disabled_when_not_ready_to_sign_up() {
        setContent(SignUpState.InputtingPhone, isReadyToSignUp = false)

        onSignUpButton().assertExists()
        onSignUpButton().assertIsNotEnabled()
    }

    @Test
    fun signup_button_is_enabled_when_ready_to_sign_up() {
        setContent(SignUpState.InputtingPhone, isReadyToSignUp = true)

        onSignUpButton().assertExists()
        onSignUpButton().assertIsEnabled()
    }

    @Test
    fun when_error_message_is_not_null_then_it_is_visible() {
        val errorMessage = "Error message"
        setContent(SignUpState.InputtingPhone, errorMessage = ErrorMessage.Raw(errorMessage))
        composeTestRule.onNodeWithText(errorMessage).assertExists()
    }

    private fun setContent(
        signUpState: SignUpState,
        isReadyToSignUp: Boolean = true,
        errorMessage: ErrorMessage? = null
    ) =
        composeTestRule.setContent {
            DefaultLinkTheme {
                SignUpBody(
                    merchantName = "Example, Inc.",
                    emailController = SimpleTextFieldController
                        .createEmailSectionController(""),
                    phoneNumberController = PhoneNumberController.createPhoneNumberController(),
                    signUpState = signUpState,
                    isReadyToSignUp = isReadyToSignUp,
                    errorMessage = errorMessage,
                    onSignUpClick = {}
                )
            }
        }

    private fun onEmailField() = composeTestRule.onNodeWithText("Email")
    private fun onProgressIndicator() = composeTestRule.onNodeWithTag(progressIndicatorTestTag)
    private fun onPhoneField() = composeTestRule.onNodeWithText("Phone number")
    private fun onSignUpButton() = composeTestRule.onNodeWithText("Join Link")
}
