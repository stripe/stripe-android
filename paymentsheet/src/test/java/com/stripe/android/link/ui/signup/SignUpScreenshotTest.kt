package com.stripe.android.link.ui.signup

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.LinkScreenshotSurface
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class SignUpScreenshotTest(
    private val testCase: TestCase
) {
    @get:Rule
    val paparazziRule = PaparazziRule(
        listOf(SystemAppearance.DarkTheme),
        listOf(FontSize.DefaultFont)
    )

    @Test
    fun testScreen() {
        paparazziRule.snapshot {
            LinkScreenshotSurface {
                SignUpBody(
                    emailController = EmailConfig.createController("test@test.com"),
                    phoneNumberController = PhoneNumberController.createPhoneNumberController("5555555555"),
                    nameController = NameConfig.createController("Jane Doe"),
                    signUpScreenState = testCase.state,
                    onSignUpClick = {}
                )
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<TestCase> {
            val signUpStates = SignUpState.entries
            val requiresNameCollectionStates = listOf(true to "RequiresNameCollection", false to "")
            val errorMessages = listOf("Something went wrong".resolvableString to "ErrorMessage", null to "")

            return signUpStates.flatMap { signUpState ->
                val signUpEnabled = signUpState == SignUpState.InputtingRemainingFields
                val signUpEnabledName = if (signUpEnabled) "SignUpEnabled" else ""
                requiresNameCollectionStates.flatMap { (requiresNameCollection, requiresNameCollectionName) ->
                    errorMessages.flatMap { (errorMessage, errorMessageName) ->
                        // Submitting is only applicable for InputtingRemainingFields.
                        val submittingStates = if (signUpState == SignUpState.InputtingRemainingFields) {
                            listOf(true to "Submitting", false to "Idle")
                        } else {
                            listOf(false to "Idle")
                        }

                        submittingStates.map { (isSubmitting, submittingStateName) ->
                            val name = buildString {
                                append("SignUpScreen")
                                append(signUpEnabledName)
                                append(signUpState.name)
                                append(requiresNameCollectionName)
                                append(submittingStateName)
                                append(errorMessageName)
                            }
                            TestCase(
                                name = name,
                                state = SignUpScreenState(
                                    merchantName = "Example Inc.",
                                    signUpEnabled = signUpEnabled,
                                    requiresNameCollection = requiresNameCollection,
                                    canEditEmail = true,
                                    signUpState = signUpState,
                                    isSubmitting = isSubmitting,
                                    errorMessage = errorMessage,
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    internal data class TestCase(val name: String, val state: SignUpScreenState) {
        override fun toString(): String = name
    }
}
