package com.stripe.android.link.ui.signup

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.ProgressIndicatorTestTag
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class SignUpBodyTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `status inputting email shows only email field`() {
        setContent(SignUpState.InputtingPrimaryField)

        onEmailField().assertExists()
        onEmailField().assertIsEnabled()
        onProgressIndicator().assertDoesNotExist()
        onPhoneField().assertDoesNotExist()
        onNameField().assertDoesNotExist()
        onSignUpButton().assertDoesNotExist()
    }

    @Test
    fun `status verifying email is disabled`() {
        setContent(SignUpState.VerifyingEmail)

        onEmailField().assertExists()
        onEmailField().assertIsNotEnabled()
        onProgressIndicator().assertExists()
        onPhoneField().assertDoesNotExist()
        onNameField().assertDoesNotExist()
        onSignUpButton().assertDoesNotExist()
    }

    @Test
    fun `status inputting phone shows all fields if name required`() {
        setContent(
            signUpState = SignUpState.InputtingRemainingFields,
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
    fun `status inputting phone shows only phone field if name not required`() {
        setContent(SignUpState.InputtingRemainingFields)

        onEmailField().assertExists()
        onEmailField().assertIsEnabled()
        onProgressIndicator().assertDoesNotExist()
        onPhoneField().assertExists()
        onPhoneField().assertIsEnabled()
        onNameField().assertDoesNotExist()
        onSignUpButton().assertExists()
    }

    @Test
    fun `header message is correct`() {
        setContent(SignUpState.InputtingPrimaryField)

        composeTestRule.onNodeWithTag(SIGN_UP_HEADER_TAG)
            .assert(hasTextExactly("Fast, secure, 1\u2060-\u2060click checkout"))
    }

    @Test
    fun `signup button is disabled when not ready to sign up`() {
        setContent(SignUpState.InputtingRemainingFields, isReadyToSignUp = false)

        onSignUpButton().assertExists()
        onSignUpButton().assertIsNotEnabled()
    }

    @Test
    fun `signup button is enabled when ready to sign up`() {
        setContent(SignUpState.InputtingRemainingFields, isReadyToSignUp = true)

        onSignUpButton().assertExists()
        onSignUpButton().assertIsEnabled()
    }

    @Test
    fun `when error message not null in state InputtingPhoneOrName then it is visible`() {
        val errorMessage = "Error message"
        setContent(SignUpState.InputtingRemainingFields, errorMessage = errorMessage.resolvableString)
        composeTestRule.onNodeWithText(errorMessage).assertExists()
    }

    @Test
    fun `when error message not null in state InputtingEmail then it is visible`() {
        val errorMessage = "Error message"
        setContent(SignUpState.InputtingPrimaryField, errorMessage = errorMessage.resolvableString)
        composeTestRule.onNodeWithText(errorMessage).assertExists()
    }

    private fun setContent(
        signUpState: SignUpState,
        isReadyToSignUp: Boolean = true,
        requiresNameCollection: Boolean = false,
        errorMessage: ResolvableString? = null
    ) = composeTestRule.setContent {
        DefaultLinkTheme {
            SignUpBody(
                emailController = EmailConfig
                    .createController(""),
                phoneNumberController = PhoneNumberController.createPhoneNumberController(),
                nameController = NameConfig
                    .createController(null),
                signUpScreenState = SignUpScreenState(
                    merchantName = "Example, Inc.",
                    signUpEnabled = isReadyToSignUp,
                    requiresNameCollection = requiresNameCollection,
                    errorMessage = errorMessage,
                    signUpState = signUpState,
                ),
                onSignUpClick = {}
            )
        }
    }

    private fun onEmailField() = composeTestRule.onNodeWithText("Email")
    private fun onProgressIndicator() = composeTestRule.onNodeWithTag(ProgressIndicatorTestTag)
    private fun onPhoneField() = composeTestRule.onNodeWithText("Phone number")
    private fun onNameField() = composeTestRule.onNodeWithText("Full name")
    private fun onSignUpButton() = composeTestRule.onNodeWithText("Agree and continue")
}
