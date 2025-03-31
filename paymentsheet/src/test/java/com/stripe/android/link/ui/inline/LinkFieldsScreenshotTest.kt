package com.stripe.android.link.ui.inline

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.SectionController
import org.junit.Rule
import org.junit.Test

class LinkFieldsScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
    )

    @Test
    fun testInputtingPrimaryField() {
        paparazziRule.snapshot {
            val focusRequester = remember { FocusRequester() }

            LinkFields(
                expanded = true,
                enabled = true,
                signUpState = SignUpState.InputtingPrimaryField,
                requiresNameCollection = false,
                errorMessage = null,
                sectionController = SectionController(null, listOf()),
                emailController = EmailConfig.createController(
                    initialValue = "email@em"
                ),
                phoneNumberController = PhoneNumberController.createPhoneNumberController(),
                nameController = NameConfig.createController(null),
                emailFocusRequester = focusRequester
            )
        }
    }

    @Test
    fun testVerifyingEmail() {
        paparazziRule.snapshot {
            val focusRequester = remember { FocusRequester() }

            LinkFields(
                expanded = true,
                enabled = true,
                signUpState = SignUpState.VerifyingEmail,
                requiresNameCollection = false,
                errorMessage = null,
                sectionController = SectionController(null, listOf()),
                emailController = EmailConfig.createController(
                    initialValue = "email@em"
                ),
                phoneNumberController = PhoneNumberController.createPhoneNumberController(),
                nameController = NameConfig.createController(null),
                emailFocusRequester = focusRequester
            )
        }
    }

    @Test
    fun testInputtingRemainingFields() {
        paparazziRule.snapshot {
            val focusRequester = remember { FocusRequester() }

            LinkFields(
                expanded = true,
                enabled = true,
                signUpState = SignUpState.InputtingRemainingFields,
                requiresNameCollection = false,
                errorMessage = null,
                sectionController = SectionController(null, listOf()),
                emailController = EmailConfig.createController(
                    initialValue = "email@em"
                ),
                phoneNumberController = PhoneNumberController.createPhoneNumberController(
                    "123456"
                ),
                nameController = NameConfig.createController(null),
                emailFocusRequester = focusRequester
            )
        }
    }

    @Test
    fun testInputtingRemainingFieldsWithName() {
        paparazziRule.snapshot {
            val focusRequester = remember { FocusRequester() }

            LinkFields(
                expanded = true,
                enabled = true,
                signUpState = SignUpState.InputtingRemainingFields,
                requiresNameCollection = true,
                errorMessage = null,
                sectionController = SectionController(null, listOf()),
                emailController = EmailConfig.createController(
                    initialValue = "email@em"
                ),
                phoneNumberController = PhoneNumberController.createPhoneNumberController(
                    "123456"
                ),
                nameController = NameConfig.createController(
                    initialValue = "James Doe"
                ),
                emailFocusRequester = focusRequester
            )
        }
    }

    @Test
    fun testErrorMessage() {
        paparazziRule.snapshot {
            val focusRequester = remember { FocusRequester() }

            LinkFields(
                expanded = true,
                enabled = true,
                signUpState = SignUpState.InputtingRemainingFields,
                requiresNameCollection = true,
                errorMessage = "Something went wrong",
                sectionController = SectionController(null, listOf()),
                emailController = EmailConfig.createController(
                    initialValue = "email@em"
                ),
                phoneNumberController = PhoneNumberController.createPhoneNumberController(
                    "123456"
                ),
                nameController = NameConfig.createController(
                    initialValue = "James Doe"
                ),
                emailFocusRequester = focusRequester
            )
        }
    }
}
