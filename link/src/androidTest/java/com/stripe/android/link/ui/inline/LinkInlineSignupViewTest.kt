package com.stripe.android.link.ui.inline

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.SimpleTextFieldController
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class LinkInlineSignupViewTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun clicking_on_checkbox_triggers_callback() {
        var count = 0
        setContent(
            expanded = false,
            toggleExpanded = {
                count++
            }
        )

        onEmailField().assertDoesNotExist()
        onProgressIndicator().assertDoesNotExist()
        onPhoneField().assertDoesNotExist()

        onSaveMyInfo().performClick()

        assertThat(count).isEqualTo(1)
    }

    @Test
    fun status_inputting_email_shows_only_email_field() {
        setContent(signUpState = SignUpState.InputtingEmail)

        onEmailField().assertExists()
        onEmailField().assertIsEnabled()
        onProgressIndicator().assertDoesNotExist()
        onPhoneField().assertDoesNotExist()
    }

    @Test
    fun status_inputting_phone_or_name_shows_all_fields_if_name_required() {
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
    }

    @Test
    fun status_inputting_phone_shows_only_phone_field_if_name_not_required() {
        setContent(signUpState = SignUpState.InputtingPhoneOrName)

        onEmailField().assertExists()
        onEmailField().assertIsEnabled()
        onProgressIndicator().assertDoesNotExist()
        onPhoneField().assertExists()
        onPhoneField().assertIsEnabled()
        onNameField().assertDoesNotExist()
    }

    @Test
    fun when_error_message_not_null_in_state_InputtingPhoneOrName_then_it_is_visible() {
        val errorMessage = "Error message"
        setContent(
            signUpState = SignUpState.InputtingPhoneOrName,
            errorMessage = ErrorMessage.Raw(errorMessage)
        )
        composeTestRule.onNodeWithText(errorMessage).assertExists()
    }

    @Test
    fun when_error_message_not_null_in_state_InputtingEmail_then_it_is_visible() {
        val errorMessage = "Error message"
        setContent(
            signUpState = SignUpState.InputtingEmail,
            errorMessage = ErrorMessage.Raw(errorMessage)
        )
        composeTestRule.onNodeWithText(errorMessage).assertExists()
    }

    @Test
    fun when_expanded_inline_logo_visible() {
        setContent(
            expanded = true
        )
        onInlineLinkLogo().assertExists()
    }

    @Test
    fun when_not_expanded_inline_logo_not_visible() {
        setContent(
            expanded = false
        )
        onInlineLinkLogo().assertDoesNotExist()
    }

    private fun setContent(
        merchantName: String = "Example, Inc.",
        emailElement: SimpleTextFieldController =
            EmailConfig.createController("email@me.co"),
        phoneController: PhoneNumberController = PhoneNumberController.createPhoneNumberController(),
        nameController: SimpleTextFieldController =
            NameConfig.createController(null),
        signUpState: SignUpState = SignUpState.InputtingEmail,
        enabled: Boolean = true,
        expanded: Boolean = true,
        requiresNameCollection: Boolean = false,
        errorMessage: ErrorMessage? = null,
        toggleExpanded: () -> Unit = {}
    ) = composeTestRule.setContent {
        DefaultLinkTheme {
            LinkInlineSignup(
                merchantName,
                emailElement,
                phoneController,
                nameController,
                signUpState,
                enabled,
                expanded,
                requiresNameCollection,
                errorMessage,
                toggleExpanded
            )
        }
    }

    private fun onEmailField() = composeTestRule.onNodeWithText("Email")
    private fun onProgressIndicator() = composeTestRule.onNodeWithTag(ProgressIndicatorTestTag)
    private fun onPhoneField() = composeTestRule.onNodeWithText("Phone number")
    private fun onNameField() = composeTestRule.onNodeWithText("Full name")
    private fun onSaveMyInfo() = composeTestRule.onNodeWithText("Save your info", substring = true)
    private fun onInlineLinkLogo() = composeTestRule.onNodeWithTag("LinkLogoIcon", useUnmergedTree = true)
}
