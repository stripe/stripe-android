package com.stripe.android.utils.screenshots

import android.graphics.Color
import android.os.Build
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.androidHome
import app.cash.paparazzi.detectEnvironment
import com.android.ide.common.rendering.api.SessionParams
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.paymentsheet.PaymentSheet
import java.lang.reflect.Field
import java.lang.reflect.Modifier

data class ComponentTestConfig(
    val systemAppearance: SystemAppearance,
    val paymentSheetAppearance: PaymentSheetAppearance,
    val fontSize: FontSize,
)

object ComponentTestConfigProvider : TestParameter.TestParameterValuesProvider {
    override fun provideValues(): List<*> {
        return PermutationsFactory.create(ComponentTestConfig::class)
    }
}

enum class SystemAppearance {
    Light, Dark,
}

enum class PaymentSheetAppearance(val appearance: PaymentSheet.Appearance) {

    Default(appearance = PaymentSheet.Appearance()),

    Custom(
        appearance = PaymentSheet.Appearance(
            colorsLight = PaymentSheet.Colors.defaultLight.copy(
                primary = Color.RED,
            ),
            colorsDark = PaymentSheet.Colors.defaultDark.copy(
                primary = Color.RED,
            ),
            shapes = PaymentSheet.Shapes.default.copy(
                cornerRadiusDp = 8f,
            ),
        ),
    ),

    Crazy(
        appearance = PaymentSheet.Appearance(
            colorsLight = PaymentSheet.Colors(
                primary = Color.MAGENTA,
                surface = Color.CYAN,
                component = Color.YELLOW,
                componentBorder = Color.RED,
                componentDivider = Color.BLACK,
                onComponent = Color.BLUE,
                onSurface = Color.GRAY,
                subtitle = Color.WHITE,
                placeholderText = Color.DKGRAY,
                appBarIcon = Color.GREEN,
                error = Color.GREEN,
            ),
            colorsDark = PaymentSheet.Colors(
                primary = Color.MAGENTA,
                surface = Color.CYAN,
                component = Color.YELLOW,
                componentBorder = Color.RED,
                componentDivider = Color.BLACK,
                onComponent = Color.BLUE,
                onSurface = Color.GRAY,
                subtitle = Color.WHITE,
                placeholderText = Color.DKGRAY,
                appBarIcon = Color.GREEN,
                error = Color.GREEN,
            ),
            shapes = PaymentSheet.Shapes(
                cornerRadiusDp = 0.0f,
                borderStrokeWidthDp = 4.0f
            ),
            // TODO
//            typography = PaymentSheet.Typography.default.copy(
//                sizeScaleFactor = 1.1f,
//                fontResId = R.font.cursive
//            )
        ),
    ),
}

enum class FontSize(val scaleFactor: Float) {
    DefaultFontSize(scaleFactor = 1f),
    LargeFontSize(scaleFactor = 1.4f),
    ExtraLargeFontSize(scaleFactor = 2f),
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
