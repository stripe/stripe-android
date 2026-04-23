package com.stripe.android.link.ui.verification

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.LinkScreenshotSurface
import com.stripe.android.model.ConsentUi
import com.stripe.android.model.LinkBrand
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
            LinkScreenshotSurface {
                VerificationBody(
                    state = testCase.content.state,
                    otpElement = testCase.content.otpElement,
                    onBack = {},
                    onResendCodeClick = {},
                    onConsentShown = {},
                    onChangeEmailClick = {},
                    didShowCodeSentNotification = {},
                    onFocusRequested = {},
                )
            }
        }
    }

    @Test
    fun testContentWithConsent() {
        paparazziRule.snapshot {
            LinkScreenshotSurface {
                val state = testCase.content.state.copy(
                    consentSection = ConsentUi.ConsentSection(
                        "By continuing, you’ll be remembered next time on <a href=''>Powdur</a>"
                    )
                )
                VerificationBody(
                    state = state,
                    otpElement = testCase.content.otpElement,
                    onBack = {},
                    onResendCodeClick = {},
                    onConsentShown = {},
                    onChangeEmailClick = {},
                    didShowCodeSentNotification = {},
                    onFocusRequested = {},
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
                            redactedPhoneNumber = "(•••) ••• ••91",
                            email = "test@test.com",
                            isProcessing = false,
                            errorMessage = null,
                            isSendingNewCode = false,
                            didSendNewCode = false,
                            defaultPayment = null,
                            isDialog = false,
                            allowLogout = true,
                            linkBrand = LinkBrand.Link,
                        )
                    )
                ),
                TestCase(
                    name = "VerificationScreenWithOTPFilled",
                    content = TestCase.Content(
                        otpElement = otpSpecWithContent(),
                        state = VerificationViewState(
                            requestFocus = false,
                            redactedPhoneNumber = "(•••) ••• ••91",
                            email = "test@test.com",
                            isProcessing = false,
                            errorMessage = null,
                            isSendingNewCode = false,
                            didSendNewCode = false,
                            defaultPayment = null,
                            isDialog = false,
                            allowLogout = true,
                            linkBrand = LinkBrand.Link,
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
                            redactedPhoneNumber = "(•••) ••• ••91",
                            email = "test@test.com",
                            errorMessage = null,
                            isSendingNewCode = false,
                            didSendNewCode = false,
                            defaultPayment = null,
                            isDialog = false,
                            allowLogout = true,
                            linkBrand = LinkBrand.Link,
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
                            redactedPhoneNumber = "(•••) ••• ••91",
                            email = "test@test.com",
                            isProcessing = false,
                            errorMessage = null,
                            didSendNewCode = false,
                            defaultPayment = null,
                            isDialog = false,
                            allowLogout = true,
                            linkBrand = LinkBrand.Link,
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
                            redactedPhoneNumber = "(•••) ••• ••91",
                            email = "test@test.com",
                            isProcessing = false,
                            errorMessage = "Something went wrong".resolvableString,
                            didSendNewCode = false,
                            defaultPayment = null,
                            isDialog = false,
                            allowLogout = true,
                            linkBrand = LinkBrand.Link,
                        )
                    )
                ),
                TestCase(
                    name = "VerificationDialogWithOTPNotFilled",
                    content = TestCase.Content(
                        otpElement = otpSpecWithContent(content = ""),
                        state = VerificationViewState(
                            requestFocus = false,
                            redactedPhoneNumber = "(•••) ••• ••91",
                            email = "test@test.com",
                            isProcessing = false,
                            errorMessage = null,
                            isSendingNewCode = false,
                            didSendNewCode = false,
                            defaultPayment = null,
                            isDialog = true,
                            allowLogout = false,
                            linkBrand = LinkBrand.Link,
                        )
                    )
                ),
                TestCase(
                    name = "VerificationDialogWithOTPFilled",
                    content = TestCase.Content(
                        otpElement = otpSpecWithContent(),
                        state = VerificationViewState(
                            requestFocus = false,
                            redactedPhoneNumber = "(•••) ••• ••91",
                            email = "test@test.com",
                            isProcessing = false,
                            errorMessage = null,
                            isSendingNewCode = false,
                            didSendNewCode = false,
                            defaultPayment = null,
                            isDialog = true,
                            allowLogout = false,
                            linkBrand = LinkBrand.Link,
                        )
                    )
                ),
                TestCase(
                    name = "VerificationDialogWithOTPFilledAndErrorMessage",
                    content = TestCase.Content(
                        otpElement = otpSpecWithContent(),
                        state = VerificationViewState(
                            isSendingNewCode = false,
                            requestFocus = false,
                            redactedPhoneNumber = "(•••) ••• ••91",
                            email = "test@test.com",
                            isProcessing = false,
                            errorMessage = "Something went wrong".resolvableString,
                            didSendNewCode = false,
                            defaultPayment = null,
                            isDialog = true,
                            allowLogout = false,
                            linkBrand = LinkBrand.Link,
                        )
                    )
                ),
                TestCase(
                    name = "VerificationScreenProcessingWebAuth",
                    content = TestCase.Content(
                        otpElement = otpSpecWithContent(content = ""),
                        state = VerificationViewState(
                            isProcessingWebAuth = true,
                            isDialog = false,
                            // Other fields shouldn't matter.
                            requestFocus = false,
                            redactedPhoneNumber = "(•••) ••• ••91",
                            email = "test@test.com",
                            isProcessing = false,
                            errorMessage = null,
                            isSendingNewCode = false,
                            didSendNewCode = false,
                            defaultPayment = null,
                            allowLogout = true,
                            linkBrand = LinkBrand.Link,
                        )
                    )
                ),
                TestCase(
                    name = "VerificationDialogProcessingWebAuth",
                    content = TestCase.Content(
                        otpElement = otpSpecWithContent(content = ""),
                        state = VerificationViewState(
                            isProcessingWebAuth = true,
                            isDialog = true,
                            // Other fields shouldn't matter.
                            requestFocus = false,
                            redactedPhoneNumber = "(•••) ••• ••91",
                            email = "test@test.com",
                            isProcessing = false,
                            errorMessage = null,
                            isSendingNewCode = false,
                            didSendNewCode = false,
                            defaultPayment = null,
                            allowLogout = true,
                            linkBrand = LinkBrand.Link,
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
