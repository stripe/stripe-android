package com.stripe.android.link.ui.signup

import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.LinkActivity
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.createAndroidIntentComposeRule
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.ui.core.elements.EmailSpec
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalAnimationApi
@RunWith(AndroidJUnit4::class)
internal class SignUpScreenTest {
    @get:Rule
    val composeTestRule = createAndroidIntentComposeRule<LinkActivity> {
        PaymentConfiguration.init(it, "publishable_key")
        Intent(it, LinkActivity::class.java).apply {
            putExtra(
                LinkActivityContract.EXTRA_ARGS,
                LinkActivityContract.Args(
                    merchantName = MERCHANT_NAME,
                    injectionParams = LinkActivityContract.Args.InjectionParams(
                        INJECTOR_KEY,
                        setOf(PRODUCT_USAGE),
                        true,
                        PUBLISHABLE_KEY,
                        STRIPE_ACCOUNT_ID
                    )
                )
            )
        }
    }

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
        onSignUpButton().assertIsNotEnabled()
    }

    @Test
    fun signup_button_is_enabled_only_when_inputs_are_valid() {
        setContent(SignUpState.InputtingPhone)

        onSignUpButton().assertExists()
        onSignUpButton().assertIsNotEnabled()

        onPhoneField().performTextInput("12345")
        onSignUpButton().assertIsNotEnabled()

        onPhoneField().performTextInput("67890")
        onSignUpButton().assertIsEnabled()
    }

    private fun setContent(signUpState: SignUpState) =
        composeTestRule.setContent {
            DefaultLinkTheme {
                SignUpBody(
                    merchantName = "Example, Inc.",
                    emailElement = EmailSpec.transform(""),
                    signUpState = signUpState,
                    onSignUpClick = {}
                )
            }
        }

    private fun onEmailField() = composeTestRule.onNodeWithText("Email")
    private fun onProgressIndicator() = composeTestRule.onNodeWithTag("CircularProgressIndicator")
    private fun onPhoneField() = composeTestRule.onNodeWithText("Mobile Number")
    private fun onSignUpButton() = composeTestRule.onNodeWithText("Join Link")

    private companion object {
        const val INJECTOR_KEY = "injectorKey"
        const val PRODUCT_USAGE = "productUsage"
        const val PUBLISHABLE_KEY = "publishableKey"
        const val STRIPE_ACCOUNT_ID = "stripeAccountId"

        const val MERCHANT_NAME = "Merchant, Inc"
    }
}
