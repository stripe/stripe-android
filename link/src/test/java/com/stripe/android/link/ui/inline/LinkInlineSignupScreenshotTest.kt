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
                errorMessage = null,
                toggleExpanded = {},
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
                errorMessage = null,
                toggleExpanded = {},
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
                errorMessage = null,
                toggleExpanded = {},
            )
        }
    }
}
