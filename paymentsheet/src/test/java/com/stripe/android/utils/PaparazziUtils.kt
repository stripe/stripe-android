package com.stripe.android.utils

import android.os.Build
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.androidHome
import app.cash.paparazzi.detectEnvironment
import com.android.ide.common.rendering.api.SessionParams
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.ui.core.PaymentsTheme
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Locale

@Composable
internal fun PaymentSheetTestTheme(
    config: ComponentTestConfig,
    content: @Composable () -> Unit,
) {

    PaymentsTheme(
        colors = PaymentsTheme.getColors(isDark = config.appearance == SystemAppearance.Dark),
    ) {
        Surface(color = MaterialTheme.colors.surface) {
            content()
        }
    }
}

data class ComponentTestConfig(
    val appearance: SystemAppearance,
//    val cornerRadius: CornerRadius,
    val fontSize: FontSize,
//    val fontFamily: FontFamily,
)

data class FullPaymentSheetTestConfig(
    val device: Device,
    val appearance: SystemAppearance,
    val cornerRadius: CornerRadius
    // TODO: Add more
)

object ComponentTestConfigProvider : TestParameter.TestParameterValuesProvider {
    override fun provideValues(): List<*> {
        return PermutationsFactory.create(ComponentTestConfig::class)
    }
}

enum class Device(val config: DeviceConfig) {
    SmallPhone(config = DeviceConfig.NEXUS_4),
    RegularPhone(config = DeviceConfig.PIXEL_5),
    Tablet(config = DeviceConfig.PIXEL_C),
}

enum class SystemAppearance {
    Light, Dark,
}

enum class FontFamily(val fontResId: Int) {
    NormalFont(fontResId = TODO()),
    CursiveFont(fontResId = TODO())
    // TODO: Add others
}

enum class FontSize(val scaleFactor: Float) {
    DefaultFontSize(scaleFactor = 1f),
    LargeFontSize(scaleFactor = 1.4f),
    ExtraLargeFontSize(scaleFactor = 2f),
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

fun ComponentTestConfig.createPaparazzi(): Paparazzi {
    makePaparazziWorkForApi33()

    return Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6.copy(
            softButtons = false,
            screenHeight = 1,
            fontScale = fontSize.scaleFactor,
        ),
        renderingMode = SessionParams.RenderingMode.V_SCROLL,
        environment = detectEnvironment().copy(
            platformDir = "${androidHome()}/platforms/android-32",
            compileSdkVersion = 32,
        ),
    )
}

private fun makePaparazziWorkForApi33() {
    val field = Build.VERSION::class.java.getField("CODENAME")
    val newValue = "REL"

    Field::class.java.getDeclaredField("modifiers").apply {
        isAccessible = true
        setInt(field, field.modifiers and Modifier.FINAL.inv())
    }

    field.apply {
        isAccessible = true
        set(null, newValue)
    }
}
