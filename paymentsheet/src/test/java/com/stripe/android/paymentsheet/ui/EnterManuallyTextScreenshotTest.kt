package com.stripe.android.paymentsheet.ui

import com.stripe.android.paymentsheet.addresselement.EnterManuallyText
import com.stripe.android.utils.screenshots.FontSize
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import com.stripe.android.utils.screenshots.SystemAppearance
import org.junit.Rule
import org.junit.Test

class EnterManuallyTextScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
    )

    @Test
    fun testDefault() {
        paparazziRule.snapshot {
            EnterManuallyText(onClick = {})
        }
    }
}
