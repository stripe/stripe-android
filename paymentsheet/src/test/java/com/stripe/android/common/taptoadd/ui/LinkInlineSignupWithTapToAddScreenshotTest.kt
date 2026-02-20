package com.stripe.android.common.taptoadd.ui

import androidx.compose.ui.Modifier
import com.stripe.android.link.ui.inline.LinkInlineSignup
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.SectionController
import org.junit.Rule
import org.junit.Test

class LinkInlineSignupWithTapToAddScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier,
        includeStripeTheme = false,
    )

    @Test
    fun default() {
        val emailController = EmailConfig.createController(
            initialValue = "email@email.com",
            showOptionalLabel = true
        )
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initialValue = "+11234567890"
        )
        val nameController = NameConfig.createController(initialValue = null)

        val sectionController = SectionController(
            label = null,
            sectionFieldValidationControllers = listOf(
                emailController,
                phoneNumberController,
                nameController,
            )
        )

        paparazziRule.snapshot {
            TapToAddTheme {
                LinkInlineSignup(
                    merchantName = "Merchant, Inc.",
                    sectionController = sectionController,
                    emailController = emailController,
                    phoneNumberController = phoneNumberController,
                    nameController = nameController,
                    signUpState = SignUpState.InputtingRemainingFields,
                    enabled = true,
                    expanded = true,
                    requiresNameCollection = true,
                    allowsDefaultOptIn = false,
                    linkSignUpOptInFeatureEnabled = false,
                    didAskToChangeSignupDetails = false,
                    errorMessage = null,
                    toggleExpanded = {},
                    changeSignupDetails = {},
                )
            }
        }
    }
}
