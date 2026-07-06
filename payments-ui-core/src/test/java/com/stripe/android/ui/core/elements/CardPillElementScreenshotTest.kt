package com.stripe.android.ui.core.elements

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.model.CardBrand
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test

class CardPillElementScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(horizontal = 4.dp)
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
    )

    @Test
    fun testVisaEnabled() {
        paparazziRule.snapshot {
            CardPillElementUI(
                enabled = true,
                cardBrand = CardBrand.Visa,
                lastFourDigits = "4242",
                onDismiss = {},
            )
        }
    }

    @Test
    fun testVisaDisabled() {
        paparazziRule.snapshot {
            CardPillElementUI(
                enabled = false,
                cardBrand = CardBrand.Visa,
                lastFourDigits = "4242",
                onDismiss = {},
            )
        }
    }

    @Test
    fun testMastercard() {
        paparazziRule.snapshot {
            CardPillElementUI(
                enabled = true,
                cardBrand = CardBrand.MasterCard,
                lastFourDigits = "4444",
                onDismiss = {},
            )
        }
    }
}
