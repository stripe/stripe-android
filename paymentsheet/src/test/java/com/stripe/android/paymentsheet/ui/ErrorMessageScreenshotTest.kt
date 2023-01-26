package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import com.stripe.android.utils.screenshots.FontSize
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import com.stripe.android.utils.screenshots.SystemAppearance
import org.junit.Rule
import org.junit.Test

class ErrorMessageScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.values(),
        FontSize.values(),
        PaymentSheetAppearance.values(),
        padding = PaddingValues(16.dp),
    )

    @Test
    fun testDefault() {
        paparazziRule.snapshot {
            ErrorMessage(error = "Something went terribly wrong")
        }
    }
}
