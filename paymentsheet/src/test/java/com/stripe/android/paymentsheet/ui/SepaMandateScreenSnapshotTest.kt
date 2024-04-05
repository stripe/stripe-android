package com.stripe.android.paymentsheet.ui

import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

internal class SepaMandateScreenSnapshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
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
