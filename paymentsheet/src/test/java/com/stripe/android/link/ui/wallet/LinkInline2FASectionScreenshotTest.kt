package com.stripe.android.link.ui.wallet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.verification.VerificationViewState
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.testing.LocaleTestRule
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class LinkInline2FASectionScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier.padding(16.dp).fillMaxWidth(),
    )

    @get:Rule
    val localeRule = LocaleTestRule(Locale.US)

    @Test
    fun testDefault() {
        val otpElement = OTPElement(
            identifier = IdentifierSpec.Generic("otp"),
            controller = OTPController()
        )

        val verificationState = VerificationViewState(
            isProcessing = false,
            requestFocus = true,
            errorMessage = null,
            isSendingNewCode = false,
            didSendNewCode = false,
            redactedPhoneNumber = "***-***-1234",
            email = "user@example.com",
            isDialog = false
        )

        paparazziRule.snapshot {
            LinkInline2FASection(
                verificationState = verificationState,
                otpElement = otpElement,
                onResend = {}
            )
        }
    }

    @Test
    fun testWithPaymentDetails() {
        val otpElement = OTPElement(
            identifier = IdentifierSpec.Generic("otp"),
            controller = OTPController()
        )

        val paymentUI = DefaultPaymentUI(
            paymentIconRes = com.stripe.android.paymentsheet.R.drawable.stripe_ic_paymentsheet_card_visa_ref,
            last4 = "4242"
        )

        val verificationState = VerificationViewState(
            isProcessing = false,
            requestFocus = true,
            errorMessage = null,
            isSendingNewCode = false,
            didSendNewCode = false,
            redactedPhoneNumber = "***-***-1234",
            email = "user@example.com",
            isDialog = false,
            defaultPayment = paymentUI
        )

        paparazziRule.snapshot {
            LinkInline2FASection(
                verificationState = verificationState,
                otpElement = otpElement,
                onResend = {}
            )
        }
    }

    @Test
    fun testWithPaymentDetailsMastercard() {
        val otpElement = OTPElement(
            identifier = IdentifierSpec.Generic("otp"),
            controller = OTPController()
        )

        val paymentUI = DefaultPaymentUI(
            paymentIconRes = com.stripe.android.paymentsheet.R.drawable.stripe_ic_paymentsheet_card_mastercard_ref,
            last4 = "5555"
        )

        val verificationState = VerificationViewState(
            isProcessing = false,
            requestFocus = true,
            errorMessage = null,
            isSendingNewCode = false,
            didSendNewCode = false,
            redactedPhoneNumber = "***-***-5678",
            email = "test@stripe.com",
            isDialog = false,
            defaultPayment = paymentUI
        )

        paparazziRule.snapshot {
            LinkInline2FASection(
                verificationState = verificationState,
                otpElement = otpElement,
                onResend = {}
            )
        }
    }

    @Test
    fun testProcessingState() {
        val otpElement = OTPElement(
            identifier = IdentifierSpec.Generic("otp"),
            controller = OTPController().apply {
                onValueChanged(0, "123456")
            }
        )

        val paymentUI = DefaultPaymentUI(
            paymentIconRes = com.stripe.android.paymentsheet.R.drawable.stripe_ic_paymentsheet_card_amex_ref,
            last4 = "0005"
        )

        val verificationState = VerificationViewState(
            isProcessing = true,
            requestFocus = false,
            errorMessage = null,
            isSendingNewCode = false,
            didSendNewCode = false,
            redactedPhoneNumber = "***-***-9999",
            email = "payment@example.com",
            isDialog = false,
            defaultPayment = paymentUI
        )

        paparazziRule.snapshot {
            LinkInline2FASection(
                verificationState = verificationState,
                otpElement = otpElement,
                onResend = {}
            )
        }
    }

    @Test
    fun testErrorState() {
        val otpElement = OTPElement(
            identifier = IdentifierSpec.Generic("otp"),
            controller = OTPController().apply {
                onValueChanged(0, "123")
            }
        )

        val verificationState = VerificationViewState(
            isProcessing = false,
            requestFocus = false,
            errorMessage = resolvableString("Invalid verification code. Please try again."),
            isSendingNewCode = false,
            didSendNewCode = false,
            redactedPhoneNumber = "***-***-1234",
            email = "user@example.com",
            isDialog = false
        )

        paparazziRule.snapshot {
            LinkInline2FASection(
                verificationState = verificationState,
                otpElement = otpElement,
                onResend = {}
            )
        }
    }

    @Test
    fun testErrorStateWithPaymentDetails() {
        val otpElement = OTPElement(
            identifier = IdentifierSpec.Generic("otp"),
            controller = OTPController().apply {
                onValueChanged(0, "999")
            }
        )

        val paymentUI = DefaultPaymentUI(
            paymentIconRes = com.stripe.android.paymentsheet.R.drawable.stripe_ic_paymentsheet_card_discover_ref,
            last4 = "1117"
        )

        val verificationState = VerificationViewState(
            isProcessing = false,
            requestFocus = false,
            errorMessage = resolvableString("Verification failed. Code expired."),
            isSendingNewCode = false,
            didSendNewCode = false,
            redactedPhoneNumber = "***-***-7890",
            email = "error@test.com",
            isDialog = false,
            defaultPayment = paymentUI
        )

        paparazziRule.snapshot {
            LinkInline2FASection(
                verificationState = verificationState,
                otpElement = otpElement,
                onResend = {}
            )
        }
    }

    @Test
    fun testSendingNewCode() {
        val otpElement = OTPElement(
            identifier = IdentifierSpec.Generic("otp"),
            controller = OTPController()
        )

        val paymentUI = DefaultPaymentUI(
            paymentIconRes = com.stripe.android.paymentsheet.R.drawable.stripe_ic_paymentsheet_card_visa_ref,
            last4 = "4242"
        )

        val verificationState = VerificationViewState(
            isProcessing = false,
            requestFocus = true,
            errorMessage = null,
            isSendingNewCode = true,
            didSendNewCode = false,
            redactedPhoneNumber = "***-***-1234",
            email = "resend@example.com",
            isDialog = false,
            defaultPayment = paymentUI
        )

        paparazziRule.snapshot {
            LinkInline2FASection(
                verificationState = verificationState,
                otpElement = otpElement,
                onResend = {}
            )
        }
    }
}
