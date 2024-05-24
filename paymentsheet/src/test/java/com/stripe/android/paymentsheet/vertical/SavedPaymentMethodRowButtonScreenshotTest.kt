package com.stripe.android.paymentsheet.vertical

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.verticalmode.SavedPaymentMethodRowButton
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import kotlin.test.Test

internal class SavedPaymentMethodRowButtonScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(16.dp)
    )

    private val savedVisa = DisplayableSavedPaymentMethod(
        displayName = "····4242",
        paymentMethod = PaymentMethod(
            id = "001",
            created = null,
            liveMode = false,
            code = PaymentMethod.Type.Card.code,
            type = PaymentMethod.Type.Card,
            card = PaymentMethod.Card(
                brand = CardBrand.Visa,
                last4 = "4242",
            )
        )
    )

    @Test
    fun testSavedVisa() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedVisa,
                resources = null,
                isEnabled = true,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedVisa_selected() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedVisa,
                resources = null,
                isEnabled = true,
                isSelected = true,
            )
        }
    }

    @Test
    fun testSavedVisa_disabled() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedVisa,
                resources = null,
                isEnabled = false,
                isSelected = false,
            )
        }
    }
}
