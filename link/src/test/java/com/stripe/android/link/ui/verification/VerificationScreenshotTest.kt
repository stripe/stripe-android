package com.stripe.android.link.ui.verification

import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.elements.OTPSpec
import com.stripe.android.uicore.elements.OTPElement
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class VerificationScreenshotTest(
    private val testCase: TestCase
) {

    @get:Rule
    val paparazziRule = PaparazziRule(
        listOf(SystemAppearance.DarkTheme),
        listOf(FontSize.DefaultFont)
    )

    @Test
    fun testContent() {
        paparazziRule.snapshot {
            VerificationBody(
                state = testCase.content.state,
                otpElement = testCase.content.otpElement,
                onBack = {},
                onResendCodeClick = {},
                onChangeEmailClick = {}
            )
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): List<TestCase> {
            return listOf(
                TestCase(
                    name = "VerificationScreenWithOTPNotFilled",
                    content = TestCase.Content(
                        otpElement = otpSpecWithContent(content = ""),
                        state = VerificationViewState(
                            requestFocus = false,
                            redactedPhoneNumber = "+1********91",
                            email = "test@test.com"
                        )
                    )
                ),
                TestCase(
                    name = "VerificationScreenWithOTPFilled",
                    content = TestCase.Content(
                        otpElement = otpSpecWithContent(),
                        state = VerificationViewState(
                            requestFocus = false,
                            redactedPhoneNumber = "+1********91",
                            email = "test@test.com"
                        )
                    )
                ),
                TestCase(
                    name = "VerificationScreenWithOTPFilledAndProcessing",
                    content = TestCase.Content(
                        otpElement = otpSpecWithContent(),
                        state = VerificationViewState(
                            isProcessing = true,
                            requestFocus = false,
                            redactedPhoneNumber = "+1********91",
                            email = "test@test.com"
                        )
                    )
                ),

                TestCase(
                    name = "VerificationScreenWithOTPFilledAndSendingNewCode",
                    content = TestCase.Content(
                        otpElement = otpSpecWithContent(),
                        state = VerificationViewState(
                            isSendingNewCode = true,
                            requestFocus = false,
                            redactedPhoneNumber = "+1********91",
                            email = "test@test.com"
                        )
                    )
                ),
            )
        }

        private fun otpSpecWithContent(content: String = "555555"): OTPElement {
            val spec = OTPSpec.transform()
            spec.controller.onAutofillDigit(content)
            return spec
        }
    }

    internal data class TestCase(val name: String, val content: Content) {
        override fun toString(): String = name

        internal data class Content(
            val otpElement: OTPElement,
            val state: VerificationViewState
        )
    }
}