package com.stripe.android.utils.screenshots

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.screenshottesting.PaparazziConfigOption
import com.stripe.android.uicore.StripeThemeDefaults

enum class PaymentSheetAppearance(val appearance: PaymentSheet.Appearance) : PaparazziConfigOption {

    DefaultAppearance(appearance = PaymentSheet.Appearance()),

    CustomAppearance(
        appearance = PaymentSheet.Appearance(
            colorsLight = PaymentSheet.Colors.configureDefaultLight(primary = Color.Red),
            colorsDark = PaymentSheet.Colors.configureDefaultDark(primary = Color.Red),
            shapes = PaymentSheet.Shapes(
                cornerRadiusDp = 8f,
                borderStrokeWidthDp = StripeThemeDefaults.shapes.borderStrokeWidth
            ),
            typography = PaymentSheet.Typography(
                fontResId = R.font.cursive,
                sizeScaleFactor = 1.75F,
            )
        ),
    ),

    @OptIn(AppearanceAPIAdditionsPreview::class)
    CrazyAppearance(
        appearance = PaymentSheet.Appearance(
            colorsLight = PaymentSheet.Colors(
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
            colorsDark = PaymentSheet.Colors(
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
            embeddedAppearance = PaymentSheet.Appearance.Embedded(

            )
        ),
    );

    override fun initialize() {
        appearance.parseAppearance()
    }

    override fun reset() {
        DefaultAppearance.appearance.parseAppearance()
    }
}
