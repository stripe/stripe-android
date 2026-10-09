package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ui.PaymentElementTheme
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

internal class AddressElementDismissalDialogScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        includeStripeTheme = false,
    )

    @Test
    fun defaultAppearance() {
        snapshot(PaymentSheetAppearance.DefaultAppearance.appearance)
    }

    @Test
    fun customAppearance() {
        snapshot(PaymentSheetAppearance.CustomAppearance.appearance)
    }

    @Test
    fun crazyAppearance() {
        snapshot(PaymentSheetAppearance.CrazyAppearance.appearance)
    }

    private fun snapshot(appearance: PaymentSheet.Appearance) {
        paparazziRule.snapshot {
            PaymentElementTheme(appearance = appearance) {
                AddressElementDismissalDialog(
                    onDiscardChanges = {},
                    onKeepEditing = {},
                )
            }
        }
    }
}
