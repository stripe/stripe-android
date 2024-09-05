package com.stripe.android.screenshottesting

import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode

interface PaparazziConfigOption {

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
