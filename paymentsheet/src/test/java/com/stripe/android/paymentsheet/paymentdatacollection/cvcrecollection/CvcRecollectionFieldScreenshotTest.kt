package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import com.stripe.android.model.CardBrand
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

class CvcRecollectionFieldScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries
    )

    @Test
    fun testEmpty() {
        paparazziRule.snapshot {
            CvcRecollectionField(
                lastFour = "4242",
                enabled = true,
                cvcState = CvcState(
                    cardBrand = CardBrand.Visa,
                    cvc = ""
                ),
                onValueChanged = {},
            )
        }
    }

    @Test
    fun testFilled() {
        paparazziRule.snapshot {
            CvcRecollectionField(
                lastFour = "4242",
                enabled = true,
                cvcState = CvcState(
                    cardBrand = CardBrand.Visa,
                    cvc = "424"
                ),
                onValueChanged = {}
            )
        }
    }
}
