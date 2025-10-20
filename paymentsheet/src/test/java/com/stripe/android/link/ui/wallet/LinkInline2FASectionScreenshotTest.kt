package com.stripe.android.link.ui.wallet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.verification.VerificationViewState
import com.stripe.android.model.DisplayablePaymentDetails
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
            defaultPayment = null,
            isDialog = false,
            allowLogout = true,
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

        val paymentDetails = DisplayablePaymentDetails(
            defaultCardBrand = "mastercard",
            last4 = "5555",
            defaultPaymentType = "CARD",
            numberOfSavedPaymentDetails = 1L
        )
        val paymentUI = paymentDetails.toDefaultPaymentUI(true)!!

        val verificationState = VerificationViewState(
            isProcessing = false,
            requestFocus = true,
            errorMessage = null,
            isSendingNewCode = false,
            didSendNewCode = false,
            redactedPhoneNumber = "***-***-5678",
            email = "test@stripe.com",
            isDialog = false,
            allowLogout = true,
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
    fun testWithPaymentDetailsBank() {
        val otpElement = OTPElement(
            identifier = IdentifierSpec.Generic("otp"),
            controller = OTPController()
        )

        val paymentDetails = DisplayablePaymentDetails(
            defaultCardBrand = null,
            last4 = "5555",
            defaultPaymentType = "BANK_ACCOUNT",
            numberOfSavedPaymentDetails = 1L
        )
        val paymentUI = paymentDetails.toDefaultPaymentUI(true)!!

        val verificationState = VerificationViewState(
            isProcessing = false,
            requestFocus = true,
            errorMessage = null,
            isSendingNewCode = false,
            didSendNewCode = false,
            redactedPhoneNumber = "***-***-5678",
            email = "test@stripe.com",
            isDialog = false,
            allowLogout = true,
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

        val paymentDetails = DisplayablePaymentDetails(
            defaultCardBrand = "amex",
            last4 = "0005",
            defaultPaymentType = "CARD",
            numberOfSavedPaymentDetails = 2L
        )
        val paymentUI = paymentDetails.toDefaultPaymentUI(true)!!

        val verificationState = VerificationViewState(
            isProcessing = true,
            requestFocus = false,
            errorMessage = null,
            isSendingNewCode = false,
            didSendNewCode = false,
            redactedPhoneNumber = "***-***-9999",
            email = "payment@example.com",
            isDialog = false,
            allowLogout = true,
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
            defaultPayment = null,
            isDialog = false,
            allowLogout = true,
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
