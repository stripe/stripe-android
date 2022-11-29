package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.addresselement.AddressElementPrimaryButton
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import com.stripe.android.utils.screenshots.SystemAppearance
import org.junit.Rule
import org.junit.Test

private const val BUTTON_TEXT = "Save Address"

class AddressElementPrimaryButtonScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.values(),
        PaymentSheetAppearance.values(),
//        FontSize.values(),
    )

    @Test
    fun testEnabled() {
        paparazziRule.snapshot(
            padding = PaddingValues(horizontal = 16.dp),
        ) {
            AddressElementPrimaryButton(isEnabled = true, BUTTON_TEXT, onButtonClick = {})
        }
    }

    @Test
    fun testDisabled() {
        paparazziRule.snapshot(
            padding = PaddingValues(horizontal = 16.dp),
        ) {
            AddressElementPrimaryButton(isEnabled = false, BUTTON_TEXT, onButtonClick = {})
        }
    }
}
