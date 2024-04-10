package com.stripe.android.uicore.elements

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test

class OTPElementUiScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        boxModifier = Modifier.padding(PaddingValues(vertical = 16.dp))
            .fillMaxWidth()
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
