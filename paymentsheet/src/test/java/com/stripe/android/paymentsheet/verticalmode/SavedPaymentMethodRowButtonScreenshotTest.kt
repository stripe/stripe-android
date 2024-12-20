package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
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

    private val savedVisa = DisplayableSavedPaymentMethod.create(
        displayName = "···· 4242".resolvableString,
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
                isEnabled = false,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedVisa_default() {

        val savedDefaultVisa = DisplayableSavedPaymentMethod.create(
            displayName = "···· 4242".resolvableString,
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
            ),
            shouldShowDefaultBadge = true,
        )

        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedDefaultVisa,
                isEnabled = false,
                isSelected = false,
            )
        }
    }
}
