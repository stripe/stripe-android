package com.stripe.android.link.ui.inline

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.testing.LocaleTestRule
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.SectionController
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class LinkInlineSignupScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier.padding(16.dp).fillMaxWidth(),
    )

    @get:Rule
    val localeRule = LocaleTestRule(Locale.US)

    @Test
    fun testCollapsed() {
        val emailController = EmailConfig.createController("email@email.com", showOptionalLabel = true)
        val phoneNumberController = PhoneNumberController.createPhoneNumberController()
        val nameController = NameConfig.createController(initialValue = null)

        val sectionController = SectionController(
            label = null,
            sectionFieldErrorControllers = listOf(
                emailController,
                phoneNumberController,
                nameController,
            )
        )

        paparazziRule.snapshot {
            LinkInlineSignup(
                merchantName = "Merchant, Inc.",
                sectionController = sectionController,
                emailController = emailController,
                phoneNumberController = phoneNumberController,
                nameController = nameController,
                signUpState = SignUpState.InputtingPrimaryField,
                enabled = true,
                expanded = false,
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

    @Test
    fun testVerifying() {
        val emailController = EmailConfig.createController("email@email.com", showOptionalLabel = true)
        val phoneNumberController = PhoneNumberController.createPhoneNumberController()
        val nameController = NameConfig.createController(initialValue = null)

        val sectionController = SectionController(
            label = null,
            sectionFieldErrorControllers = listOf(
                emailController,
                phoneNumberController,
                nameController,
            )
        )

        paparazziRule.snapshot {
            LinkInlineSignup(
                merchantName = "Merchant, Inc.",
                sectionController = sectionController,
                emailController = emailController,
                phoneNumberController = phoneNumberController,
                nameController = nameController,
                signUpState = SignUpState.VerifyingEmail,
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

    @Test
    fun testExpanded() {
        val emailController = EmailConfig.createController("email@email.com", showOptionalLabel = true)
        val phoneNumberController = PhoneNumberController.createPhoneNumberController()
        val nameController = NameConfig.createController(initialValue = null)

        val sectionController = SectionController(
            label = null,
            sectionFieldErrorControllers = listOf(
                emailController,
                phoneNumberController,
                nameController,
            )
        )

        paparazziRule.snapshot {
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

    @Test
    fun testDefaultOptIn() {
        val emailController = EmailConfig.createController("email@email.com", showOptionalLabel = true)
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(initialValue = "5555555555")
        val nameController = NameConfig.createController(initialValue = null)

        val sectionController = SectionController(
            label = null,
            sectionFieldErrorControllers = listOf(
                emailController,
                phoneNumberController,
                nameController,
            )
        )

        paparazziRule.snapshot {
            LinkInlineSignup(
                merchantName = "Merchant, Inc.",
                sectionController = sectionController,
                emailController = emailController,
                phoneNumberController = phoneNumberController,
                nameController = nameController,
                signUpState = SignUpState.InputtingRemainingFields,
                enabled = true,
                expanded = true,
                requiresNameCollection = false,
                allowsDefaultOptIn = true,
                didAskToChangeSignupDetails = false,
                linkSignUpOptInFeatureEnabled = false,
                errorMessage = null,
                toggleExpanded = {},
                changeSignupDetails = {},
            )
        }
    }

    @Test
    fun testDefaultOptInWithPartialValues() {
        val emailController = EmailConfig.createController("email@email.com", showOptionalLabel = true)
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(initialValue = "")
        val nameController = NameConfig.createController(initialValue = null)

        val sectionController = SectionController(
            label = null,
            sectionFieldErrorControllers = listOf(
                emailController,
                phoneNumberController,
                nameController,
            )
        )

        paparazziRule.snapshot {
            LinkInlineSignup(
                merchantName = "Merchant, Inc.",
                sectionController = sectionController,
                emailController = emailController,
                phoneNumberController = phoneNumberController,
                nameController = nameController,
                signUpState = SignUpState.InputtingRemainingFields,
                enabled = true,
                expanded = true,
                requiresNameCollection = false,
                allowsDefaultOptIn = true,
                didAskToChangeSignupDetails = false,
                linkSignUpOptInFeatureEnabled = false,
                errorMessage = null,
                toggleExpanded = {},
                changeSignupDetails = {},
            )
        }
    }

    @Test
    fun testDefaultOptInAfterChangingSignupData() {
        val emailController = EmailConfig.createController("email@email.com", showOptionalLabel = true)
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(initialValue = "5555555555")
        val nameController = NameConfig.createController(initialValue = null)

        val sectionController = SectionController(
            label = null,
            sectionFieldErrorControllers = listOf(
                emailController,
                phoneNumberController,
                nameController,
            )
        )

        paparazziRule.snapshot {
            LinkInlineSignup(
                merchantName = "Merchant, Inc.",
                sectionController = sectionController,
                emailController = emailController,
                phoneNumberController = phoneNumberController,
                nameController = nameController,
                signUpState = SignUpState.InputtingRemainingFields,
                enabled = true,
                expanded = true,
                requiresNameCollection = false,
                allowsDefaultOptIn = true,
                linkSignUpOptInFeatureEnabled = false,
                didAskToChangeSignupDetails = true,
                errorMessage = null,
                toggleExpanded = {},
                changeSignupDetails = {},
            )
        }
    }

    @Test
    fun testLinkInlineSignupCheckbox() {
        val emailController = EmailConfig.createController("email@email.com", showOptionalLabel = true)
        val phoneNumberController = PhoneNumberController.createPhoneNumberController()
        val nameController = NameConfig.createController(initialValue = null)

        val sectionController = SectionController(
            label = null,
            sectionFieldErrorControllers = listOf(
                emailController,
                phoneNumberController,
                nameController,
            )
        )

        paparazziRule.snapshot {
            LinkInlineSignup(
                merchantName = "Merchant, Inc.",
                sectionController = sectionController,
                emailController = emailController,
                phoneNumberController = phoneNumberController,
                nameController = nameController,
                signUpState = SignUpState.InputtingPrimaryField,
                enabled = true,
                expanded = false,
                requiresNameCollection = true,
                allowsDefaultOptIn = false,
                linkSignUpOptInFeatureEnabled = true,
                didAskToChangeSignupDetails = false,
                errorMessage = null,
                toggleExpanded = {},
                changeSignupDetails = {},
            )
        }
    }
}
