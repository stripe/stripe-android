package com.stripe.android.link.ui.verification

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.screenshottesting.PaparazziRule
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
    val paparazziRule = PaparazziRule()

    @Test
    fun testContent() {
        paparazziRule.snapshot {
            DefaultLinkTheme {
                VerificationBody(
                    state = testCase.content.state,
                    otpElement = testCase.content.otpElement,
                    onBack = {},
                    onResendCodeClick = {},
                    onChangeEmailClick = {}
                )
            }
        }
    }

    companion object {
        @SuppressWarnings("LongMethod")
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
                            email = "test@test.com",
                            isProcessing = false,
                            errorMessage = null,
                            isSendingNewCode = false,
                            didSendNewCode = false
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
                            email = "test@test.com",
                            isProcessing = false,
                            errorMessage = null,
                            isSendingNewCode = false,
                            didSendNewCode = false
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
                            email = "test@test.com",
                            errorMessage = null,
                            isSendingNewCode = false,
                            didSendNewCode = false
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
                            email = "test@test.com",
                            isProcessing = false,
                            errorMessage = null,
                            didSendNewCode = false
                        )
                    )
                ),
                TestCase(
                    name = "VerificationScreenWithOTPFilledAndErrorMessage",
                    content = TestCase.Content(
                        otpElement = otpSpecWithContent(),
                        state = VerificationViewState(
                            isSendingNewCode = false,
                            requestFocus = false,
                            redactedPhoneNumber = "+1********91",
                            email = "test@test.com",
                            isProcessing = false,
                            errorMessage = "Something went wrong".resolvableString,
                            didSendNewCode = false
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
