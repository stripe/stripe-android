package com.stripe.android.uicore.elements

import com.stripe.android.uicore.utils.PaparazziRule
import com.stripe.android.uicore.utils.SystemAppearance
import org.junit.Rule
import org.junit.Test

class OTPElementUiScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.values()
    )

    @Test
    fun testOtpElementEnabled() {
        paparazziRule.snapshot {
            OTPElementUI(
                enabled = true,
                element = OTPElement(
                    identifier = IdentifierSpec.Generic("otp"),
                    controller = OTPController()
                )
            )
        }
    }

    @Test
    fun testOtpElementDisabled() {
        paparazziRule.snapshot {
            OTPElementUI(
                enabled = false,
                element = OTPElement(
                    identifier = IdentifierSpec.Generic("otp"),
                    controller = OTPController()
                )
            )
        }
    }
}
