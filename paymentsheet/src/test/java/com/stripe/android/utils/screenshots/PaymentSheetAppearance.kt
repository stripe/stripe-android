package com.stripe.android.utils.screenshots

import androidx.compose.ui.graphics.Color
import com.stripe.android.elements.Appearance
import com.stripe.android.elements.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.screenshottesting.PaparazziConfigOption
import com.stripe.android.uicore.StripeThemeDefaults

enum class PaymentSheetAppearance(val appearance: Appearance) : PaparazziConfigOption {

    DefaultAppearance(appearance = Appearance()),

    CustomAppearance(
        appearance = Appearance(
            colorsLight = Appearance.Colors.configureDefaultLight(primary = Color.Red),
            colorsDark = Appearance.Colors.configureDefaultDark(primary = Color.Red),
            shapes = Appearance.Shapes(
                cornerRadiusDp = 8f,
                borderStrokeWidthDp = StripeThemeDefaults.shapes.borderStrokeWidth
            ),
            typography = Appearance.Typography(
                fontResId = R.font.cursive,
                sizeScaleFactor = 1.75F,
            )
        ),
    ),

    @OptIn(AppearanceAPIAdditionsPreview::class)
    CrazyAppearance(
        appearance = Appearance(
            colorsLight = Appearance.Colors(
                primary = Color.Magenta,
                surface = Color.Cyan,
                component = Color.Yellow,
                componentBorder = Color.Red,
                componentDivider = Color.Black,
                onComponent = Color.Blue,
                onSurface = Color.Gray,
                subtitle = Color.White,
                placeholderText = Color.DarkGray,
                appBarIcon = Color.Green,
                error = Color.Green,
            ),
            colorsDark = Appearance.Colors(
                primary = Color.Magenta,
                surface = Color.Cyan,
                component = Color.Yellow,
                componentBorder = Color.Red,
                componentDivider = Color.Black,
                onComponent = Color.Blue,
                onSurface = Color.Gray,
                subtitle = Color.White,
                placeholderText = Color.DarkGray,
                appBarIcon = Color.Green,
                error = Color.Green,
            ),
            shapes = Appearance.Shapes(
                cornerRadiusDp = 0.0f,
                borderStrokeWidthDp = 4.0f
            ),
            primaryButton = Appearance.PrimaryButton(
                shape = Appearance.PrimaryButton.Shape(
                    heightDp = 96f
                )
            ),
            formInsetValues = Appearance.Insets(
                startDp = 40f,
                topDp = 20f,
                endDp = 40f,
                bottomDp = 60f
            ),
            typography = Appearance.Typography(
                sizeScaleFactor = 1f,
                fontResId = null,
                custom = Appearance.Typography.Custom(
                    h1 = Appearance.Typography.Font(
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
