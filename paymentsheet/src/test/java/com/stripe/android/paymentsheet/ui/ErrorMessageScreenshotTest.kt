package com.stripe.android.paymentsheet.ui

import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test

class ErrorMessageScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        PaymentSheetAppearance.entries,
    )

    @Test
    fun testDefault() {
        paparazziRule.snapshot {
            ErrorMessage(error = "Something went terribly wrong")
        }
    }
}
