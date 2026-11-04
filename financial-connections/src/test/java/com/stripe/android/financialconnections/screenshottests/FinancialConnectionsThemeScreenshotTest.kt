package com.stripe.android.financialconnections.screenshottests

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.NightMode
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.financialconnections.ThemeMode
import com.stripe.android.financialconnections.features.consent.ConsentPreviewParameterProvider
import com.stripe.android.financialconnections.features.consent.ContentPreview
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.Theme
import com.stripe.android.financialconnections.ui.theme.toNightMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class FinancialConnectionsThemeScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi()

    @Test
    fun forcedTheme(
        @TestParameter forcedTheme: ForcedTheme,
        @TestParameter theme: Theme,
    ) {
        val nightMode = when (forcedTheme.themeMode.toNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> NightMode.NIGHT
            AppCompatDelegate.MODE_NIGHT_NO -> NightMode.NOTNIGHT
            else -> forcedTheme.systemNightMode
        }
        paparazzi.unsafeUpdateConfig(
            DeviceConfig.NEXUS_5.copy(softButtons = false, nightMode = nightMode),
        )
        val preview = ConsentPreviewParameterProvider().values.elementAt(
            if (theme == Theme.LinkLight) 3 else 2,
        )
        paparazzi.snapshot {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                FinancialConnectionsTheme(theme) {
                    Box(Modifier.background(FinancialConnectionsTheme.colors.background)) {
                        ContentPreview(preview)
                    }
                }
            }
        }
    }

    enum class ForcedTheme(val themeMode: ThemeMode, val systemNightMode: NightMode) {
        LightOnDarkSystem(ThemeMode.AlwaysLight, NightMode.NIGHT),
        DarkOnLightSystem(ThemeMode.AlwaysDark, NightMode.NOTNIGHT),
    }
}
