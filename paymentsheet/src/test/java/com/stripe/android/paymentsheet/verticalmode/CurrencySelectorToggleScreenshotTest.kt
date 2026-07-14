package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.checkout.Checkout
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class CurrencySelectorToggleScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier.padding(all = 16.dp),
    )

    private val defaultAppearance = Checkout.CurrencySelectorContentAppearance().build()

    private val options = CurrencySelectorOptions(
        first = CurrencyOption(code = "USD", formattedAmount = "$50.99", flag = FlagContent.Emoji("🇺🇸")),
        second = CurrencyOption(code = "EUR", formattedAmount = "€45.87", flag = FlagContent.Emoji("🇪🇺")),
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
                showCurrencyCode = false,
                appearance = defaultAppearance,
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
                showCurrencyCode = false,
                appearance = defaultAppearance,
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
                showCurrencyCode = false,
                appearance = defaultAppearance,
            )
        }
    }

    @Test
    fun testErrorMessage() {
        paparazziRule.snapshot {
            CurrencySelectorToggle(
                options = options,
                onCurrencySelected = {},
                isEnabled = true,
                showCurrencyCode = false,
                errorMessage = "Something went wrong. Try again.",
                appearance = defaultAppearance,
            )
        }
    }
}
