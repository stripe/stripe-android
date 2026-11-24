package com.stripe.android.link.ui.verification

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.link.ui.LinkScreenshotSurface
import com.stripe.android.link.ui.wallet.LinkInline2FASection
import com.stripe.android.model.LinkBrand
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.elements.OTPElementFactory
import com.stripe.android.uicore.elements.PhoneNumberController
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class VerificationSandboxScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(SystemAppearance.entries, FontSize.entries)

    @Test
    fun sandbox(@TestParameter presentation: Presentation) {
        val state = VerificationViewState(
            isTestMode = true,
            authFlow = null,
            isProcessing = false,
            requestFocus = false,
            errorMessage = null,
            isSendingNewCode = false,
            didSendNewCode = false,
            redactedPhoneNumber = "(•••) ••• ••42",
            email = "jane@example.com",
            isDialog = presentation == Presentation.Dialog,
            allowLogout = false,
            defaultPayment = null,
            linkBrand = LinkBrand.Link,
        )
        val otpElement = OTPElementFactory.create()
        paparazziRule.snapshot {
            LinkScreenshotSurface {
                if (presentation == Presentation.Inline) {
                    LinkInline2FASection(
                        verificationState = state,
                        otpElement = otpElement,
                        onResend = {},
                    )
                } else {
                    VerificationBody(
                        state = state,
                        otpElement = otpElement,
                        phoneNumberController = PhoneNumberController.createPhoneNumberController(),
                        onBack = {},
                        onNavigateBack = {},
                        onFocusRequested = {},
                        didShowCodeSentNotification = {},
                        onChangeEmailClick = {},
                        onResendCodeClick = {},
                        onEmailCodeClick = {},
                        onPhoneNumberSubmitted = {},
                        onConsentShown = {},
                    )
                }
            }
        }
    }

    enum class Presentation { Screen, Dialog, Inline }
}
