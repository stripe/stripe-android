package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

@OptIn(AppearanceAPIAdditionsPreview::class)
internal class TapToAddThemeScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        boxModifier = Modifier.fillMaxSize(),
        includeStripeTheme = false,
    )

    @Test
    fun automaticTheme() {
        snapshotWithAppearance(PaymentSheet.Appearance())
    }

    @Test
    fun alwaysLightTheme() {
        snapshotWithAppearance(
            PaymentSheet.Appearance(themeMode = PaymentSheet.ThemeMode.AlwaysLight),
        )
    }

    @Test
    fun alwaysDarkTheme() {
        snapshotWithAppearance(
            PaymentSheet.Appearance(themeMode = PaymentSheet.ThemeMode.AlwaysDark),
        )
    }

    @Test
    fun customAppearanceTheme() {
        snapshotWithAppearance(PaymentSheetAppearance.CrazyAppearance.appearance)
    }

    private fun snapshotWithAppearance(appearance: PaymentSheet.Appearance) {
        paparazziRule.snapshot {
            TapToAddTheme(
                appearance = appearance,
                imageRepository = null,
            ) {
                TapToAddLayout(
                    screen = TapToAddNavigator.Screen.CardAdded(
                        interactor = FakeTapToAddCardAddedInteractor(),
                    ),
                    onCancel = {},
                )
            }
        }
    }
}
