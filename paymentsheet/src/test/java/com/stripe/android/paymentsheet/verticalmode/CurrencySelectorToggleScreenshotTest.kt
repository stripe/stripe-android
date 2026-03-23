package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

internal class CurrencySelectorToggleScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier.padding(horizontal = 16.dp),
    )

    private val options = CurrencySelectorOptions(
        first = CurrencyOption(code = "USD", displayableText = "\uD83C\uDDFA\uD83C\uDDF8 $50.99"),
        second = CurrencyOption(code = "EUR", displayableText = "\uD83C\uDDEA\uD83C\uDDFA €45.87"),
        selectedCode = "USD",
        exchangeRateText = "1 USD = 0.91 EUR",
    )

    @Test
    fun testFirstOptionSelected() {
        paparazziRule.snapshot {
            CurrencySelectorToggle(
                options = options,
                onCurrencySelected = {},
                isEnabled = true,
            )
        }
    }

    @Test
    fun testSecondOptionSelected() {
        paparazziRule.snapshot {
            CurrencySelectorToggle(
                options = options.copy(selectedCode = "EUR", exchangeRateText = null),
                onCurrencySelected = {},
                isEnabled = true,
            )
        }
    }

    @Test
    fun testDisabled() {
        paparazziRule.snapshot {
            CurrencySelectorToggle(
                options = options,
                onCurrencySelected = {},
                isEnabled = false,
            )
        }
    }
}
