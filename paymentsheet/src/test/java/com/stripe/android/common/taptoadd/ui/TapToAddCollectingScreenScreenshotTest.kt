package com.stripe.android.common.taptoadd.ui

import androidx.compose.ui.Modifier
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import org.junit.Test

internal class TapToAddCollectingScreenScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier,
        includeStripeTheme = false,
    )

    @Test
    fun default() {
        paparazziRule.snapshot {
            TapToAddTheme {
                TapToAddCollectingScreen()
            }
        }
    }
}
