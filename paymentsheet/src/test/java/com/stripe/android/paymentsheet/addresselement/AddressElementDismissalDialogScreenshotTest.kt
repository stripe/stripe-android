package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

internal class AddressElementDismissalDialogScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
    )

    @Test
    fun dialog() {
        paparazziRule.snapshot {
            AddressElementDismissalDialog(
                onDiscardChanges = {},
                onKeepEditing = {},
            )
        }
    }
}
