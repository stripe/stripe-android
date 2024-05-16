package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.PaymentSheet
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

    private val viewModel = CvcRecollectionViewModel(
        CvcRecollectionViewModel.Args(
            lastFour = "4242",
            cardBrand = CardBrand.Visa,
        )
    )

    @Test
    fun testEmpty() {
        paparazziRule.snapshot {
            CvcRecollectionScreen(
                viewModel = viewModel
            )
        }
    }

    @Test
    fun testFilled() {
        paparazziRule.snapshot {
            CvcRecollectionScreen(
                viewModel = viewModel
            )
        }
    }
}
