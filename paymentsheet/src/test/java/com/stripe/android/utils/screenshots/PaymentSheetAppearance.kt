package com.stripe.android.utils.screenshots

import android.graphics.Color
import com.stripe.android.paymentelement.ExtendedAppearancePreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.screenshottesting.PaparazziConfigOption

enum class PaymentSheetAppearance(val appearance: PaymentSheet.Appearance) : PaparazziConfigOption {

    DefaultAppearance(appearance = PaymentSheet.Appearance()),

    CustomAppearance(
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
            typography = PaymentSheet.Typography(
                fontResId = R.font.cursive,
                sizeScaleFactor = 1.75F,
            )
        ),
    ),

    @OptIn(ExtendedAppearancePreview::class)
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
            primaryButton = PaymentSheet.PrimaryButton(
                shape = PaymentSheet.PrimaryButtonShape(
                    heightDp = 96f
                )
            ),
            formInsetValues = PaymentSheet.Insets(
                startDp = 40f,
                topDp = 20f,
                endDp = 40f,
                bottomDp = 60f
            ),
            typography = PaymentSheet.Typography(
                sizeScaleFactor = 1f,
                fontResId = null,
                custom = PaymentSheet.Typography.Custom(
                    h1 = PaymentSheet.Typography.Font(
                        fontFamily = R.font.cursive,
                        fontSizeSp = 24f,
                        fontWeight = 700,
                        letterSpacingSp = 0.15f,
                    )
                )
            ),
        ),
    );

    override fun initialize() {
        appearance.parseAppearance()
    }

    override fun reset() {
        DefaultAppearance.appearance.parseAppearance()
    }
}
