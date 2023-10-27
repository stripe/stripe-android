package com.stripe.android.view

import android.graphics.Color
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.model.CardBrand
import com.stripe.android.utils.PaparazziRule
import com.stripe.android.utils.SystemAppearance
import org.junit.Rule
import org.junit.Test

class CardBrandScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        configOptions = arrayOf(SystemAppearance.values()),
        boxModifier = Modifier.padding(16.dp),
    )

    @Test
    fun testUnknownBrand() {
        paparazziRule.snapshot {
            CardBrand(
                isLoading = false,
                currentBrand = CardBrand.Unknown,
                possibleBrands = emptyList(),
                shouldShowCvc = false,
                shouldShowErrorIcon = false,
                tintColorInt = Color.GRAY,
                isCbcEligible = false,
                onBrandSelected = {},
            )
        }
    }

    @Test
    fun testKnownBrand() {
        paparazziRule.snapshot {
            CardBrand(
                isLoading = false,
                currentBrand = CardBrand.CartesBancaires,
                possibleBrands = emptyList(),
                shouldShowCvc = false,
                shouldShowErrorIcon = false,
                tintColorInt = Color.GRAY,
                isCbcEligible = false,
                onBrandSelected = {},
            )
        }
    }

    @Test
    fun testWithCbcEligible() {
        paparazziRule.snapshot {
            CardBrand(
                isLoading = false,
                currentBrand = CardBrand.Unknown,
                possibleBrands = listOf(CardBrand.CartesBancaires, CardBrand.Visa),
                shouldShowCvc = false,
                shouldShowErrorIcon = false,
                tintColorInt = Color.GRAY,
                isCbcEligible = true,
                onBrandSelected = {},
            )
        }
    }

    @Test
    fun testWithErrorAndCbcEligible() {
        paparazziRule.snapshot {
            CardBrand(
                isLoading = false,
                currentBrand = CardBrand.Unknown,
                possibleBrands = listOf(CardBrand.CartesBancaires, CardBrand.Visa),
                shouldShowCvc = false,
                shouldShowErrorIcon = true,
                tintColorInt = Color.GRAY,
                isCbcEligible = true,
                onBrandSelected = {},
            )
        }
    }

    @Test
    fun testWithCvcAndCbcEligible() {
        paparazziRule.snapshot {
            CardBrand(
                isLoading = false,
                currentBrand = CardBrand.Unknown,
                possibleBrands = listOf(CardBrand.CartesBancaires, CardBrand.Visa),
                shouldShowCvc = true,
                shouldShowErrorIcon = false,
                tintColorInt = Color.GRAY,
                isCbcEligible = true,
                onBrandSelected = {},
            )
        }
    }
}
