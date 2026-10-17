package com.stripe.android.common.nfcscan.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.stripe.android.common.nfcscan.tapzone.TapZone
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

@OptIn(AppearanceAPIAdditionsPreview::class)
internal class NfcScanningThemeScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        boxModifier = Modifier.fillMaxSize(),
        includeStripeTheme = false,
    )

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
        paparazziRule.snapshot {
            NfcScanningTheme(appearance = appearance) {
                NfcScanningLayout(
                    status = NfcScanningStatus.Idle(),
                    tapZone = TapZone(xBias = 0.5f, yBias = 0.3f),
                    deviceRotation = DeviceRotation.Portrait,
                    onClose = {},
                    onSuccessShown = {},
                    onErrorShown = {},
                )
            }
        }
    }
}
