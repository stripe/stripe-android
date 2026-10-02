package com.stripe.android.link.ui.verification

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.LinkScreenshotSurface
import com.stripe.android.model.ConsentUi
import com.stripe.android.model.LinkBrand
import com.stripe.android.screenshottesting.LayoutDirection
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.elements.OTPElementFactory
import com.stripe.android.uicore.elements.PhoneNumberController
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class VerificationScreenshotTest(
    private val testCase: TestCase
) {

    @get:Rule
    val paparazziRule = PaparazziRule(LayoutDirection.entries)

    @Test
    fun testContent() {
        paparazziRule.snapshot {
            LinkScreenshotSurface {
                VerificationBody(
                    state = testCase.content.state,
                    otpElement = testCase.content.otpElement,
                    phoneNumberController = PhoneNumberController.createPhoneNumberController(),
                    onBack = {},
                    onNavigateBack = {},
                    onEmailCodeClick = {},
                    onPhoneNumberSubmitted = {},
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
                    phoneNumberController = PhoneNumberController.createPhoneNumberController(),
                    onBack = {},
                    onNavigateBack = {},
                    onEmailCodeClick = {},
                    onPhoneNumberSubmitted = {},
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
                        otpElement = otpElementWithContent(content = ""),
                        state = VerificationViewState(
                            authFlow = null,
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
                        otpElement = otpElementWithContent(),
                        state = VerificationViewState(
                            authFlow = null,
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
                        otpElement = otpElementWithContent(),
                        state = VerificationViewState(
                            authFlow = null,
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
                        otpElement = otpElementWithContent(),
                        state = VerificationViewState(
                            authFlow = null,
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
                        otpElement = otpElementWithContent(),
                        state = VerificationViewState(
                            authFlow = null,
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
                        otpElement = otpElementWithContent(content = ""),
                        state = VerificationViewState(
                            authFlow = null,
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
                        otpElement = otpElementWithContent(),
                        state = VerificationViewState(
                            authFlow = null,
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
                        otpElement = otpElementWithContent(),
                        state = VerificationViewState(
                            authFlow = null,
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
                        otpElement = otpElementWithContent(content = ""),
                        state = VerificationViewState(
                            authFlow = null,
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
                        otpElement = otpElementWithContent(content = ""),
                        state = VerificationViewState(
                            authFlow = null,
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
                authFlowTestCase(
                    name = "VerificationScreenAuthFlowMoreOptions",
                    authFlow = authFlowState(
                        actions = listOf(LinkAuthFlowState.Action.Resend, LinkAuthFlowState.Action.Email),
                    ),
                ),
                authFlowTestCase(
                    name = "VerificationScreenAuthFlowEmailResendCooldown",
                    authFlow = authFlowState(
                        recipient = "test@test.com",
                        canResend = false,
                        resendSecondsRemaining = 7,
                    ),
                ),
                authFlowTestCase(
                    name = "VerificationScreenAuthFlowPhoneMatch",
                    authFlow = authFlowState(screen = LinkAuthFlowState.Screen.PhoneMatch),
                ),
                authFlowTestCase(
                    name = "VerificationScreenAuthFlowPhoneMatchError",
                    authFlow = authFlowState(screen = LinkAuthFlowState.Screen.PhoneMatch),
                    errorMessage = "The phone number doesn't match this account.",
                ),
                authFlowTestCase(
                    name = "VerificationScreenAuthFlowBlocked",
                    authFlow = authFlowState(screen = LinkAuthFlowState.Screen.Blocked),
                    errorMessage = "We couldn't verify your account. Please close this window and try again.",
                ),
                authFlowTestCase(
                    name = "VerificationDialogAuthFlowCanGoBack",
                    authFlow = authFlowState(recipient = "test@test.com", canGoBack = true),
                    isDialog = true,
                ),
            )
        }

        private fun authFlowTestCase(
            name: String,
            authFlow: VerificationViewState.AuthFlowViewState,
            errorMessage: String? = null,
            isDialog: Boolean = false,
        ) = TestCase(
            name = name,
            content = TestCase.Content(
                otpElement = otpElementWithContent(content = ""),
                state = VerificationViewState(
                    authFlow = authFlow,
                    requestFocus = false,
                    redactedPhoneNumber = "(•••) ••• ••91",
                    email = "test@test.com",
                    isProcessing = false,
                    errorMessage = errorMessage?.resolvableString,
                    isSendingNewCode = false,
                    didSendNewCode = false,
                    defaultPayment = null,
                    isDialog = isDialog,
                    allowLogout = true,
                    linkBrand = LinkBrand.Link,
                )
            )
        )

        private fun authFlowState(
            screen: LinkAuthFlowState.Screen = LinkAuthFlowState.Screen.Otp,
            recipient: String = "(•••) ••• ••91",
            canGoBack: Boolean = false,
            actions: List<LinkAuthFlowState.Action> = listOf(LinkAuthFlowState.Action.Resend),
            canResend: Boolean = true,
            resendSecondsRemaining: Int = 0,
        ) = VerificationViewState.AuthFlowViewState(
            screen = screen,
            recipient = recipient,
            canGoBack = canGoBack,
            codeEntryEnabled = true,
            actions = actions,
            canResend = canResend,
            isResending = false,
            resendSecondsRemaining = resendSecondsRemaining,
            phoneNumberLastTwoDigits = "91",
        )

        private fun otpElementWithContent(content: String = "555555"): OTPElement {
            val element = OTPElementFactory.create()
            element.controller.onAutofillDigit(content)
            return element
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
