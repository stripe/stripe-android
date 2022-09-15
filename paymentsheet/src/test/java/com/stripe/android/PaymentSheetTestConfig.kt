package com.stripe.android

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Environment
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.androidHome
import app.cash.paparazzi.detectEnvironment
import com.google.testing.junit.testparameterinjector.TestParameter
import java.util.Locale

data class PaymentSheetTestConfig(
    val device: Device,
    val appearance: SystemAppearance,
    val cornerRadius: CornerRadius
    // TODO: Add more
)

fun PaymentSheetTestConfig.createPaparazzi(): Paparazzi {
    return Paparazzi(
        deviceConfig = device.config.copy(softButtons = false),
        maxPercentDifference = 0.0,
        environment = stripeTestEnvironment()
    )
}

private fun stripeTestEnvironment(): Environment = detectEnvironment().copy(
    // LayoutLib doesn't support SDK 33 yet, so we need to run on 32
    platformDir = "${androidHome()}/platforms/android-32",
    compileSdkVersion = 32
)

object PaymentSheetTestConfigProvider : TestParameter.TestParameterValuesProvider {
    override fun provideValues(): List<*> {
        return PermutationsFactory.create(PaymentSheetTestConfig::class)
    }
}

enum class Device(val config: DeviceConfig) {
    SmallPhone(config = DeviceConfig.NEXUS_4),
    RegularPhone(config = DeviceConfig.PIXEL_5),
    Tablet(config = DeviceConfig.PIXEL_C)
}

enum class SystemAppearance {
    Light, Dark
}

enum class FontFamily(val fontResId: Int) {
    NormalFont(fontResId = TODO()),
    CursiveFont(fontResId = TODO())
    // TODO: Add others
}

enum class FontSize(val scaleFactor: Float) {
    SmallFontSize(scaleFactor = 0.6f),
    DefaultFontSize(scaleFactor = 1f),
    LargeFontSize(scaleFactor = 1.5f),
    ExtraLargeFontSize(scaleFactor = 3f)
}

enum class CornerRadius(val value: Float) {
    NoCornerRadius(value = 0f),
    NormalCornerRadius(value = 4f),
    LargeCornerRadius(value = 16f),
    ExtraLargeCornerRadius(value = 32f)
}

enum class Language(val locale: Locale) {
    ENGLISH(locale = Locale.ENGLISH),
    GERMAN(locale = Locale.GERMAN)
    // TODO: Add others
}
