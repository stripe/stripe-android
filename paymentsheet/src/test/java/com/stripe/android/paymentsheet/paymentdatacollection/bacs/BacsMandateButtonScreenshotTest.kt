package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import com.stripe.android.utils.screenshots.FontSize
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import com.stripe.android.utils.screenshots.SystemAppearance
import org.junit.Rule
import org.junit.Test

class BacsMandateButtonScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.values(),
        PaymentSheetAppearance.values(),
        FontSize.values()
    )

    @Test
    fun testPrimary() {
        paparazziRule.snapshot {
            BacsMandateButton(
                label = "Confirm",
                type = BacsMandateButtonType.Primary,
                onClick = {}
            )
        }
    }

    @Test
    fun testSecondary() {
        paparazziRule.snapshot {
            BacsMandateButton(
                label = "Modify details",
                type = BacsMandateButtonType.Secondary,
                onClick = {}
            )
        }
    }
}
