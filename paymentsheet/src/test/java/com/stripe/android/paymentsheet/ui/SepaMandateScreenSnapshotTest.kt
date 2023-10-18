package com.stripe.android.paymentsheet.ui

import com.stripe.android.utils.screenshots.FontSize
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import com.stripe.android.utils.screenshots.SystemAppearance
import org.junit.Rule
import org.junit.Test

internal class SepaMandateScreenSnapshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.values(),
        PaymentSheetAppearance.values(),
        FontSize.values(),
    )

    @Test
    fun snapshot() {
        paparazziRule.snapshot {
            SepaMandateScreen(
                merchantName = "Example, Inc.",
                acknowledgedCallback = {},
                closeCallback = {},
            )
        }
    }
}
