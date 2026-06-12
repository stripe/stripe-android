package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stripe.android.checkout.Checkout
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class CurrencySelectorToggleAppearanceScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier.padding(all = 16.dp),
    )

    private val options = CurrencySelectorOptions(
        first = CurrencyOption(code = "USD", displayableText = "🇺🇸 \$50.99", formattedAmount = "\$50.99"),
        second = CurrencyOption(code = "EUR", displayableText = "🇪🇺 €45.87", formattedAmount = "€45.87"),
        selectedCode = "USD",
        exchangeRateText = "1 USD = 0.91 EUR",
    )

    @Test
    fun testCustomColors() {
        val appearance = Checkout.CurrencySelectorContentAppearance()
            .selectedBackground(Color(0xFF6200EE))
            .background(Color(0xFFE8DEF8))
            .selectedTextColor(Color.White)
            .textColor(Color(0xFF6200EE))
            .borderColor(Color(0xFF6200EE))
            .borderWidthDp(2f)
            .textSecondaryColor(Color(0xFF49454F))
            .build()

        paparazziRule.snapshot {
            CurrencySelectorToggle(
                options = options,
                onCurrencySelected = {},
                isEnabled = true,
                appearance = appearance,
            )
        }
    }

    @Test
    fun testCustomDimensions() {
        val appearance = Checkout.CurrencySelectorContentAppearance()
            .contentVerticalPaddingDp(12f)
            .cornerRadiusDp(8f)
            .borderWidthDp(1f)
            .build()

        paparazziRule.snapshot {
            CurrencySelectorToggle(
                options = options,
                onCurrencySelected = {},
                isEnabled = true,
                appearance = appearance,
            )
        }
    }

    @Test
    fun testLargeSizeScaleFactor() {
        val appearance = Checkout.CurrencySelectorContentAppearance()
            .sizeScaleFactor(1.4f)
            .contentVerticalPaddingDp(8f)
            .build()

        paparazziRule.snapshot {
            CurrencySelectorToggle(
                options = options,
                onCurrencySelected = {},
                isEnabled = true,
                appearance = appearance,
            )
        }
    }

    @Test
    fun testSmallSizeScaleFactor() {
        val appearance = Checkout.CurrencySelectorContentAppearance()
            .sizeScaleFactor(0.8f)
            .contentVerticalPaddingDp(2f)
            .build()

        paparazziRule.snapshot {
            CurrencySelectorToggle(
                options = options,
                onCurrencySelected = {},
                isEnabled = true,
                appearance = appearance,
            )
        }
    }

    @Test
    fun testCustomDangerColor() {
        val appearance = Checkout.CurrencySelectorContentAppearance()
            .dangerColor(Color(0xFFB3261E))
            .build()

        paparazziRule.snapshot {
            CurrencySelectorToggle(
                options = options,
                onCurrencySelected = {},
                isEnabled = true,
                errorMessage = "Currency unavailable. Try again.",
                appearance = appearance,
            )
        }
    }

    @Test
    fun testNoBorder() {
        val appearance = Checkout.CurrencySelectorContentAppearance()
            .borderWidthDp(0f)
            .build()

        paparazziRule.snapshot {
            CurrencySelectorToggle(
                options = options,
                onCurrencySelected = {},
                isEnabled = true,
                appearance = appearance,
            )
        }
    }

    @Test
    fun testSquareCorners() {
        val appearance = Checkout.CurrencySelectorContentAppearance()
            .cornerRadiusDp(0f)
            .borderWidthDp(1f)
            .build()

        paparazziRule.snapshot {
            CurrencySelectorToggle(
                options = options,
                onCurrencySelected = {},
                isEnabled = true,
                appearance = appearance,
            )
        }
    }

    @Test
    fun testCustomTextSecondaryColor() {
        val appearance = Checkout.CurrencySelectorContentAppearance()
            .textSecondaryColor(Color(0xFF006B5E))
            .build()

        paparazziRule.snapshot {
            CurrencySelectorToggle(
                options = options,
                onCurrencySelected = {},
                isEnabled = true,
                appearance = appearance,
            )
        }
    }

    @Test
    fun testSecondOptionSelectedWithCustomColors() {
        val appearance = Checkout.CurrencySelectorContentAppearance()
            .selectedBackground(Color(0xFF006B5E))
            .selectedTextColor(Color.White)
            .background(Color(0xFFD0F5EE))
            .textColor(Color(0xFF002019))
            .build()

        paparazziRule.snapshot {
            CurrencySelectorToggle(
                options = options.copy(selectedCode = "EUR"),
                onCurrencySelected = {},
                isEnabled = true,
                appearance = appearance,
            )
        }
    }
}
