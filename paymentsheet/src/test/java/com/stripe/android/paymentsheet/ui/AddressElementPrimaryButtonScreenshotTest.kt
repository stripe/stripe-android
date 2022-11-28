package com.stripe.android.paymentsheet.ui

import com.stripe.android.paymentsheet.addresselement.AddressElementPrimaryButton
import com.stripe.android.utils.screenshots.FontSize2
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance2
import com.stripe.android.utils.screenshots.SystemAppearance2
import org.junit.Rule
import org.junit.Test

private const val BUTTON_TEXT = "Save Address"

class AddressElementPrimaryButtonScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance2.values(),
        PaymentSheetAppearance2.values(),
        FontSize2.values(),
    )

    @Test
    fun testEnabled() {
        paparazziRule.snapshot {
            AddressElementPrimaryButton(isEnabled = true, BUTTON_TEXT, onButtonClick = {})
        }
    }

    @Test
    fun testDisabled() {
        paparazziRule.snapshot {
            AddressElementPrimaryButton(isEnabled = false, BUTTON_TEXT, onButtonClick = {})
        }
    }
}
