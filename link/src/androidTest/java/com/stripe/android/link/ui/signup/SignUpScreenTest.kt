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
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
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
        onNameField().assertDoesNotExist()
        onSignUpButton().assertDoesNotExist()
    }

    @Test
    fun status_verifying_email_is_disabled() {
        setContent(SignUpState.VerifyingEmail)

        onEmailField().assertExists()
        onEmailField().assertIsNotEnabled()
        onProgressIndicator().assertExists()
        onPhoneField().assertDoesNotExist()
        onNameField().assertDoesNotExist()
        onSignUpButton().assertDoesNotExist()
    }

    @Test
    fun status_inputting_phone_shows_all_fields_if_name_required() {
        setContent(
            signUpState = SignUpState.InputtingPhoneOrName,
            requiresNameCollection = true
        )

        onEmailField().assertExists()
        onEmailField().assertIsEnabled()
        onProgressIndicator().assertDoesNotExist()
        onPhoneField().assertExists()
        onPhoneField().assertIsEnabled()
        onNameField().assertExists()
        onNameField().assertIsEnabled()
        onSignUpButton().assertExists()
    }

    @Test
    fun status_inputting_phone_shows_only_phone_field_if_name_not_required() {
        setContent(SignUpState.InputtingPhoneOrName)

        onEmailField().assertExists()
        onEmailField().assertIsEnabled()
        onProgressIndicator().assertDoesNotExist()
        onPhoneField().assertExists()
        onPhoneField().assertIsEnabled()
        onNameField().assertDoesNotExist()
        onSignUpButton().assertExists()
    }

    @Test
    fun header_message_is_correct() {
        setContent(SignUpState.InputtingEmail)

        composeTestRule.onNodeWithText("Secure 1\u2060-\u2060click checkout").assertExists()
    }

    @Test
    fun signup_button_is_disabled_when_not_ready_to_sign_up() {
        setContent(SignUpState.InputtingPhoneOrName, isReadyToSignUp = false)

        onSignUpButton().assertExists()
        onSignUpButton().assertIsNotEnabled()
    }

    @Test
    fun signup_button_is_enabled_when_ready_to_sign_up() {
        setContent(SignUpState.InputtingPhoneOrName, isReadyToSignUp = true)

        onSignUpButton().assertExists()
        onSignUpButton().assertIsEnabled()
    }

    @Test
    fun when_error_message_not_null_in_state_InputtingPhoneOrName_then_it_is_visible() {
        val errorMessage = "Error message"
        setContent(SignUpState.InputtingPhoneOrName, errorMessage = ErrorMessage.Raw(errorMessage))
        composeTestRule.onNodeWithText(errorMessage).assertExists()
    }

    @Test
    fun when_error_message_not_null_in_state_InputtingEmail_then_it_is_visible() {
        val errorMessage = "Error message"
        setContent(SignUpState.InputtingEmail, errorMessage = ErrorMessage.Raw(errorMessage))
        composeTestRule.onNodeWithText(errorMessage).assertExists()
    }

    private fun setContent(
        signUpState: SignUpState,
        isReadyToSignUp: Boolean = true,
        requiresNameCollection: Boolean = false,
        errorMessage: ErrorMessage? = null
    ) = composeTestRule.setContent {
        DefaultLinkTheme {
            SignUpBody(
                merchantName = "Example, Inc.",
                emailController = EmailConfig
                    .createController(""),
                phoneNumberController = PhoneNumberController.createPhoneNumberController(),
                nameController = NameConfig
                    .createController(null),
                signUpState = signUpState,
                isReadyToSignUp = isReadyToSignUp,
                requiresNameCollection = requiresNameCollection,
                errorMessage = errorMessage,
                onSignUpClick = {}
            )
        }
    }

    private fun onEmailField() = composeTestRule.onNodeWithText("Email")
    private fun onProgressIndicator() = composeTestRule.onNodeWithTag(progressIndicatorTestTag)
    private fun onPhoneField() = composeTestRule.onNodeWithText("Phone number")
    private fun onNameField() = composeTestRule.onNodeWithText("Full name")
    private fun onSignUpButton() = composeTestRule.onNodeWithText("Join Link")
}
