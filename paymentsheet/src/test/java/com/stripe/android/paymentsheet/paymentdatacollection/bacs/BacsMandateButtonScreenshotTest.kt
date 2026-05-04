package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.PaparazziTest
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(PaparazziTest::class)
class BacsMandateButtonScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries
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
