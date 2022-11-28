package com.stripe.android.utils.screenshots

import android.graphics.Color
import app.cash.paparazzi.DeviceConfig
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.parseAppearance

interface PaparazziConfigOption {

    fun apply(deviceConfig: DeviceConfig): DeviceConfig = deviceConfig

    fun initialize() {
        // Nothing to do
    }
}

enum class SystemAppearance : PaparazziConfigOption {
    Light, Dark;
}

enum class FontSize(val scaleFactor: Float) : PaparazziConfigOption {
    Default(scaleFactor = 1f),
    Large(scaleFactor = 1.2f),
    ExtraLarge(scaleFactor = 1.5f);

    override fun apply(deviceConfig: DeviceConfig): DeviceConfig {
        return deviceConfig.copy(
            fontScale = scaleFactor,
        )
    }
}

enum class PaymentSheetAppearance(val appearance: PaymentSheet.Appearance) : PaparazziConfigOption {

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
            // TODO Move to stripe-test
//            typography = PaymentSheet.Typography.default.copy(
//                sizeScaleFactor = 1.1f,
//                fontResId = R.font.cursive
//            )
        ),
    );

    override fun initialize() {
        appearance.parseAppearance()
    }
}
