package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.addresselement.AddressElementPrimaryButton
import com.stripe.android.utils.screenshots.FontSize
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
        FontSize.values(),
        boxModifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
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
