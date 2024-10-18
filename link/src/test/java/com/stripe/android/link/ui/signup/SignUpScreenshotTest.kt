package com.stripe.android.link.ui.signup

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import org.junit.Rule
import org.junit.Test

internal class SignUpScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
//        PaymentSheetAppearance.entries,
        FontSize.entries
    )

    @Test
    fun sign_up_screen_with_signup_and_name_collection_enabled() {
        paparazziRule.snapshot {
            SignUpBody(
                emailController = EmailConfig.createController("test@test.com"),
                phoneNumberController = PhoneNumberController.createPhoneNumberController("5555555555"),
                nameController = NameConfig.createController("Jane Doe"),
                signUpScreenState = SignUpScreenState(
                    signUpEnabled = true,
                    signUpState = SignUpState.InputtingPrimaryField,
                    merchantName = "Example Inc.",
                    requiresNameCollection = true
                ),
            ) {
            }
        }
    }

    @Test
    fun status_inputting_email_shows_only_email_field2() {
        paparazziRule.snapshot {
            SignUpBody(
                emailController = EmailConfig.createController("test@test.com"),
                phoneNumberController = PhoneNumberController.createPhoneNumberController("5555555555"),
                nameController = NameConfig.createController("Jane Doe"),
                signUpScreenState = SignUpScreenState(
                    signUpEnabled = false,
                    signUpState = SignUpState.InputtingPrimaryField,
                    merchantName = "Example Inc.",
                    requiresNameCollection = true
                ),
            ) {
            }
        }
    }

    @Test
    fun status_inputting_email_shows_only_email_field3() {
        paparazziRule.snapshot {
            SignUpBody(
                emailController = EmailConfig.createController("test@test.com"),
                phoneNumberController = PhoneNumberController.createPhoneNumberController("5555555555"),
                nameController = NameConfig.createController("Jane Doe"),
                signUpScreenState = SignUpScreenState(
                    signUpEnabled = false,
                    signUpState = SignUpState.InputtingPrimaryField,
                    merchantName = "Example Inc.",
                    requiresNameCollection = false
                ),
            ) {
            }
        }
    }

    @Test
    fun status_inputting_email_shows_only_email_field4() {
        paparazziRule.snapshot {
            SignUpBody(
                emailController = EmailConfig.createController("test@test.com"),
                phoneNumberController = PhoneNumberController.createPhoneNumberController("5555555555"),
                nameController = NameConfig.createController("Jane Doe"),
                signUpScreenState = SignUpScreenState(
                    signUpEnabled = false,
                    signUpState = SignUpState.InputtingPrimaryField,
                    merchantName = "Example Inc.",
                    requiresNameCollection = true,
                    errorMessage = "Something went wrrong".resolvableString
                ),
            ) {
            }
        }
    }

    @Test
    fun status_inputting_email_shows_only_email_field5() {
        paparazziRule.snapshot {
            SignUpBody(
                emailController = EmailConfig.createController("test@test.com"),
                phoneNumberController = PhoneNumberController.createPhoneNumberController("5555555555"),
                nameController = NameConfig.createController("Jane Doe"),
                signUpScreenState = SignUpScreenState(
                    signUpEnabled = false,
                    signUpState = SignUpState.VerifyingEmail,
                    merchantName = "Example Inc.",
                    requiresNameCollection = true,
                    errorMessage = "Something went wrrong".resolvableString
                ),
            ) {
            }
        }
    }

    @Test
    fun status_inputting_email_shows_only_email_field6() {
        paparazziRule.snapshot {
            SignUpBody(
                emailController = EmailConfig.createController("test@test.com"),
                phoneNumberController = PhoneNumberController.createPhoneNumberController("5555555555"),
                nameController = NameConfig.createController("Jane Doe"),
                signUpScreenState = SignUpScreenState(
                    signUpEnabled = false,
                    signUpState = SignUpState.InputtingRemainingFields,
                    merchantName = "Example Inc.",
                    requiresNameCollection = true,
                    errorMessage = "Something went wrong".resolvableString
                ),
            ) {
            }
        }
    }
}
