package com.stripe.android.screenshottesting

import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalLayoutDirection
import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import com.android.resources.ScreenOrientation
import androidx.compose.ui.unit.LayoutDirection as ComposeLayoutDirection
import com.android.resources.LayoutDirection as AndroidLayoutDirection

interface PaparazziConfigOption {

    fun compositionLocalValues(): List<ProvidedValue<*>> = emptyList()

    fun apply(deviceConfig: DeviceConfig): DeviceConfig = deviceConfig

    fun initialize() {
        // Do nothing by default.
    }

    fun reset() {
        // Do nothing by default.
    }
}

enum class SystemAppearance(private val nightMode: NightMode) : PaparazziConfigOption {
    LightTheme(NightMode.NOTNIGHT),
    DarkTheme(NightMode.NIGHT);

    override fun apply(deviceConfig: DeviceConfig): DeviceConfig {
        return deviceConfig.copy(nightMode = nightMode)
    }
}

enum class FontSize(val scaleFactor: Float) : PaparazziConfigOption {
    DefaultFont(scaleFactor = 1f),
    LargeFont(scaleFactor = 1.5f);

    override fun apply(deviceConfig: DeviceConfig): DeviceConfig {
        return deviceConfig.copy(
            fontScale = scaleFactor,
        )
    }
}

enum class Locale(val locale: String) : PaparazziConfigOption {
    UnitedStates(locale = "us"),
    France(locale = "fr"),
    Finland(locale = "fi");

    override fun apply(deviceConfig: DeviceConfig): DeviceConfig {
        return deviceConfig.copy(
            locale = locale
        )
    }
}

enum class LayoutDirection(
    private val androidLayoutDirection: AndroidLayoutDirection,
    private val composeLayoutDirection: ComposeLayoutDirection,
) : PaparazziConfigOption {
    LeftToRight(AndroidLayoutDirection.LTR, ComposeLayoutDirection.Ltr),
    RightToLeft(AndroidLayoutDirection.RTL, ComposeLayoutDirection.Rtl);

    override fun compositionLocalValues(): List<ProvidedValue<*>> {
        return listOf(LocalLayoutDirection provides composeLayoutDirection)
    }

    override fun apply(deviceConfig: DeviceConfig): DeviceConfig {
        return deviceConfig.copy(layoutDirection = androidLayoutDirection)
    }
}

enum class Orientation(private val orientation: ScreenOrientation) : PaparazziConfigOption {
    Portrait(ScreenOrientation.PORTRAIT),
    Landscape(ScreenOrientation.LANDSCAPE);

    override fun apply(deviceConfig: DeviceConfig): DeviceConfig {
        return deviceConfig.copy(
            orientation = orientation,
        )
    }
}
