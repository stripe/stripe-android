package com.stripe.android.common.taptoadd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import com.stripe.android.common.taptoadd.ui.TapToAddCardAddedScreen
import com.stripe.android.common.taptoadd.ui.TapToAddTheme
import com.stripe.android.model.CardBrand
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import org.junit.Test

class TapToAddCardAddedScreenScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule()

    @Test
    fun default() {
        paparazziRule.gif(
            end = 3500L
        ) {
            TapToAddTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.background)
                ) {
                    TapToAddCardAddedScreen(
                        cardBrand = CardBrand.Visa,
                        last4 = "4242"
                    ) { }
                }
            }
        }
    }
}
