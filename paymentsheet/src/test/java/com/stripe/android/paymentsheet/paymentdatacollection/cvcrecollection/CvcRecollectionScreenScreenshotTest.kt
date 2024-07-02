package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import com.stripe.android.model.CardBrand
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

class CvcRecollectionScreenScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries
    )

    @Test
    fun testEmpty() {
        paparazziRule.snapshot {
            CvcRecollectionScreen(
                cardBrand = CardBrand.Visa,
                lastFour = "4242",
                isTestMode = false,
                viewActionHandler = {}
            )
        }
    }

    @Test
    fun testFilled() {
        paparazziRule.snapshot {
            CvcRecollectionScreen(
                cardBrand = CardBrand.Visa,
                lastFour = "4242",
                isTestMode = false,
                viewActionHandler = {}
            )
        }
    }

    @Test
    fun testFilledTestMode() {
        paparazziRule.snapshot {
            CvcRecollectionScreen(
                cardBrand = CardBrand.Visa,
                lastFour = "4242",
                isTestMode = true,
                viewActionHandler = {}
            )
        }
    }
}
