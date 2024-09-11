package com.stripe.android.utils.screenshots

import android.graphics.Color
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.screenshottesting.PaparazziConfigOption
import com.stripe.android.paymentsheet.R

enum class PaymentSheetAppearance(val appearance: PaymentSheet.Appearance) : PaparazziConfigOption {

    DefaultAppearance(appearance = PaymentSheet.Appearance()),

    CrazyAppearance(
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
        ),
    ),

    CustomFontAppearance(
        appearance = PaymentSheet.Appearance(
            typography = PaymentSheet.Typography(
                sizeScaleFactor = 1.75F,
                fontResId = R.font.cursive,
            )
        )
    );

    override fun initialize() {
        appearance.parseAppearance()
    }

    override fun reset() {
        DefaultAppearance.appearance.parseAppearance()
    }
}
