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

class LinkOptionalInlineSignupScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(vertical = 16.dp)
            .fillMaxWidth(),
    )

    @get:Rule
    val localeRule = LocaleTestRule(Locale.US)

    @Test
    fun testEmailFirstCollapsed() {
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
            LinkOptionalInlineSignup(
                sectionController = sectionController,
                emailController = emailController,
                phoneNumberController = phoneNumberController,
                nameController = nameController,
                isShowingPhoneFirst = false,
                signUpState = SignUpState.InputtingPrimaryField,
                enabled = true,
                requiresNameCollection = true,
                errorMessage = null,
            )
        }
    }

    @Test
    fun testEmailFirstVerifying() {
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
            LinkOptionalInlineSignup(
                sectionController = sectionController,
                emailController = emailController,
                phoneNumberController = phoneNumberController,
                nameController = nameController,
                isShowingPhoneFirst = false,
                signUpState = SignUpState.VerifyingEmail,
                enabled = true,
                requiresNameCollection = true,
                errorMessage = null,
            )
        }
    }

    @Test
    fun testEmailFirstExpanded() {
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
            LinkOptionalInlineSignup(
                sectionController = sectionController,
                emailController = emailController,
                phoneNumberController = phoneNumberController,
                nameController = nameController,
                isShowingPhoneFirst = false,
                signUpState = SignUpState.InputtingRemainingFields,
                enabled = true,
                requiresNameCollection = true,
                errorMessage = null,
            )
        }
    }

    @Test
    fun testPhoneFirstCollapsed() {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initialValue = "+15555555555",
            showOptionalLabel = true,
        )
        val emailController = EmailConfig.createController("email@email.com", showOptionalLabel = true)
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
            LinkOptionalInlineSignup(
                sectionController = sectionController,
                emailController = emailController,
                phoneNumberController = phoneNumberController,
                nameController = nameController,
                isShowingPhoneFirst = true,
                signUpState = SignUpState.InputtingPrimaryField,
                enabled = true,
                requiresNameCollection = true,
                errorMessage = null,
            )
        }
    }

    @Test
    fun testPhoneFirstExpanded() {
        val phoneNumberController = PhoneNumberController.createPhoneNumberController(
            initialValue = "+15555555555",
            showOptionalLabel = true,
        )
        val emailController = EmailConfig.createController(initialValue = null)
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
            LinkOptionalInlineSignup(
                sectionController = sectionController,
                emailController = emailController,
                phoneNumberController = phoneNumberController,
                nameController = nameController,
                isShowingPhoneFirst = true,
                signUpState = SignUpState.InputtingRemainingFields,
                enabled = true,
                requiresNameCollection = true,
                errorMessage = null,
            )
        }
    }
}
