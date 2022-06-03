package com.stripe.android.link.ui.inline

import android.content.Intent
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.LinkActivity
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.StripeIntentFixtures
import com.stripe.android.link.createAndroidIntentComposeRule
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.progressIndicatorTestTag
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.ui.core.elements.PhoneNumberController
import com.stripe.android.ui.core.elements.SimpleTextFieldController
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class LinkInlineSignupViewTest {
    @get:Rule
    val composeTestRule = createAndroidIntentComposeRule<LinkActivity> {
        PaymentConfiguration.init(it, "publishable_key")
        Intent(it, LinkActivity::class.java).apply {
            putExtra(
                LinkActivityContract.EXTRA_ARGS,
                LinkActivityContract.Args(
                    StripeIntentFixtures.PI_SUCCEEDED,
                    true,
                    "Merchant, Inc"
                )
            )
        }
    }

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
    fun status_inputting_phone_shows_all_fields() {
        setContent(signUpState = SignUpState.InputtingPhone)

        onEmailField().assertExists()
        onEmailField().assertIsEnabled()
        onProgressIndicator().assertDoesNotExist()
        onPhoneField().assertExists()
        onPhoneField().assertIsEnabled()
    }

    private fun setContent(
        merchantName: String = "Example, Inc.",
        emailElement: SimpleTextFieldController = SimpleTextFieldController.createEmailSectionController(
            "email@me.co"
        ),
        phoneController: PhoneNumberController = PhoneNumberController.createPhoneNumberController(),
        signUpState: SignUpState = SignUpState.InputtingEmail,
        enabled: Boolean = true,
        expanded: Boolean = true,
        toggleExpanded: () -> Unit = {},
        onUserInteracted: () -> Unit = {}
    ) = composeTestRule.setContent {
        DefaultLinkTheme {
            LinkInlineSignup(
                merchantName,
                emailElement,
                phoneController,
                signUpState,
                enabled,
                expanded,
                toggleExpanded,
                onUserInteracted
            )
        }
    }

    private fun onEmailField() = composeTestRule.onNodeWithText("Email")
    private fun onProgressIndicator() = composeTestRule.onNodeWithTag(progressIndicatorTestTag)
    private fun onPhoneField() = composeTestRule.onNodeWithText("Phone number")
    private fun onSaveMyInfo() = composeTestRule.onNodeWithText("Save my info", substring = true)
}
