package com.stripe.android.link.ui.wallet

import com.stripe.android.link.ui.verification.VerificationViewState
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

internal class LinkInline2FASectionSnapshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
    )

    @Test
    fun snapshot() {
        paparazziRule.snapshot {
            LinkInline2FASection(
                verificationState = VerificationViewState(
                    isProcessing = false,
                    requestFocus = false,
                    errorMessage = null,
                    isSendingNewCode = false,
                    didSendNewCode = false,
                    redactedPhoneNumber = "***-***-1234",
                    email = "user@example.com",
                    isDialog = false
                ),
                otpElement = OTPElement(
                    identifier = IdentifierSpec.Generic("otp"),
                    controller = OTPController()
                ),
                onResend = {},
                appearance = null
            )
        }
    }

    @Test
    fun snapshot_processing() {
        paparazziRule.snapshot {
            LinkInline2FASection(
                verificationState = VerificationViewState(
                    isProcessing = true,
                    requestFocus = false,
                    errorMessage = null,
                    isSendingNewCode = false,
                    didSendNewCode = false,
                    redactedPhoneNumber = "***-***-1234",
                    email = "user@example.com",
                    isDialog = false
                ),
                otpElement = OTPElement(
                    identifier = IdentifierSpec.Generic("otp"),
                    controller = OTPController().apply {
                        onValueChanged(0, "123456")
                    }
                ),
                onResend = {},
                appearance = null
            )
        }
    }
}
