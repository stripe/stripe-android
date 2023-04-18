package com.stripe.android.uicore.utils

import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode

interface PaparazziConfigOption {

    fun apply(deviceConfig: DeviceConfig): DeviceConfig = deviceConfig

    fun initialize() {
        // Nothing to do
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
