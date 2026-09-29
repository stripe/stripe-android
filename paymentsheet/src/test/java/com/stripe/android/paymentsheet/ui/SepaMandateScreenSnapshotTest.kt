package com.stripe.android.paymentsheet.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

@OptIn(AppearanceAPIAdditionsPreview::class)
internal class SepaMandateScreenSnapshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
    )

    @get:Rule
    val scopedThemePaparazziRule = PaparazziRule(
        SystemAppearance.entries,
        includeStripeTheme = false,
    )

    @Test
    fun snapshot() {
        paparazziRule.snapshot {
            TestSepaMandateScreen()
        }
    }

    @Test
    fun testAutomaticTheme() {
        snapshotWithAppearance(PaymentSheet.Appearance())
    }

    @Test
    fun testAlwaysLightTheme() {
        snapshotWithAppearance(
            PaymentSheet.Appearance(themeMode = PaymentSheet.ThemeMode.AlwaysLight),
        )
    }

    @Test
    fun testAlwaysDarkTheme() {
        snapshotWithAppearance(
            PaymentSheet.Appearance(themeMode = PaymentSheet.ThemeMode.AlwaysDark),
        )
    }

    @Test
    fun testCustomAppearanceTheme() {
        snapshotWithAppearance(PaymentSheetAppearance.CrazyAppearance.appearance)
    }

    private fun snapshotWithAppearance(appearance: PaymentSheet.Appearance) {
        scopedThemePaparazziRule.snapshot {
            PaymentElementTheme(appearance = appearance) {
                Surface(color = MaterialTheme.colors.surface) {
                    TestSepaMandateScreen()
                }
            }
        }
    }

    @Composable
    private fun TestSepaMandateScreen() {
        SepaMandateScreen(
            merchantName = "Example, Inc.",
            acknowledgedCallback = {},
            closeCallback = {},
        )
    }
}
