package com.stripe.android.paymentsheet.ui

import com.stripe.android.utils.screenshots.FontSize2
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance2
import com.stripe.android.utils.screenshots.SystemAppearance2
import org.junit.Rule
import org.junit.Test

class GooglePayDividerScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance2.values(),
        PaymentSheetAppearance2.values(),
        FontSize2.values(),
    )

    @Test
    fun testDefault() {
        paparazziRule.snapshot {
            GooglePayDividerUi()
        }
    }
}
