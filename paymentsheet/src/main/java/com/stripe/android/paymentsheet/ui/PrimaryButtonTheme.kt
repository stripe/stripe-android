package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getBackgroundColor
import com.stripe.android.uicore.getBorderStrokeColor
import com.stripe.android.uicore.getOnBackgroundColor

internal data class PrimaryButtonColors(
    val background: Color = Color.Unspecified,
    val onBackground: Color = Color.Unspecified,
    val successBackground: Color = Color.Unspecified,
    val onSuccessBackground: Color = Color.Unspecified,
    val border: Color = Color.Unspecified,
)

internal data class PrimaryButtonShape(
    val cornerRadius: Dp = Dp.Unspecified,
    val borderStrokeWidth: Dp = Dp.Unspecified
)

internal data class PrimaryButtonTypography(
    val fontFamily: FontFamily? = null,
    val fontSize: TextUnit = TextUnit.Unspecified,
)

internal val LocalPrimaryButtonColors = staticCompositionLocalOf {
    PrimaryButtonColors()
}

internal val LocalPrimaryButtonShape = staticCompositionLocalOf {
    PrimaryButtonShape()
}

internal val LocalPrimaryButtonTypography = staticCompositionLocalOf {
    PrimaryButtonTypography()
}

internal object PrimaryButtonTheme {
    val colors: PrimaryButtonColors
        @Composable
        get() = getPrimaryButtonColors()

    val shape: PrimaryButtonShape
        @Composable
        get() = getPrimaryButtonShape()

    val typography: PrimaryButtonTypography
        @Composable
        get() = getPrimaryButtonTypography()

    @Composable
    private fun getPrimaryButtonColors(): PrimaryButtonColors {
        val style = StripeTheme.primaryButtonStyle
        val context = LocalContext.current
        val localColors = LocalPrimaryButtonColors.current
        val isDarkTheme = isSystemInDarkTheme()

        val defaultSuccessBackground =
            colorResource(id = R.color.stripe_paymentsheet_primary_button_success_background)

        return remember(
            style,
            context,
            localColors,
            isDarkTheme
        ) {
            PrimaryButtonColors(
                background = if (localColors.background.isUnspecified) {
                    Color(style.getBackgroundColor(context))
                } else {
                    localColors.background
                },
                onBackground = if (localColors.onBackground.isUnspecified) {
                    Color(style.getOnBackgroundColor(context))
                } else {
                    localColors.onBackground
                },
                successBackground = if (localColors.successBackground.isUnspecified) {
                    defaultSuccessBackground
                } else {
                    localColors.successBackground
                },
                onSuccessBackground = if (localColors.onSuccessBackground.isUnspecified) {
                    if (isDarkTheme) {
                        Color.Black
                    } else {
                        Color.White
                    }
                } else {
                    localColors.onSuccessBackground
                },
                border = if (localColors.border.isUnspecified) {
                    Color(style.getBorderStrokeColor(context))
                } else {
                    localColors.border
                }
            )
        }
    }

    @Composable
    private fun getPrimaryButtonShape(): PrimaryButtonShape {
        val style = StripeTheme.primaryButtonStyle
        val localShape = LocalPrimaryButtonShape.current

        return remember(style, localShape) {
            PrimaryButtonShape(
                cornerRadius = if (localShape.cornerRadius.isUnspecified) {
                    style.shape.cornerRadius.dp
                } else {
                    localShape.cornerRadius
                },
                borderStrokeWidth = if (localShape.borderStrokeWidth.isUnspecified) {
                    style.shape.borderStrokeWidth.dp
                } else {
                    localShape.borderStrokeWidth
                }
            )
        }
    }

    @Composable
    private fun getPrimaryButtonTypography(): PrimaryButtonTypography {
        val style = StripeTheme.primaryButtonStyle
        val localTypography = LocalPrimaryButtonTypography.current

        return remember(style, localTypography) {
            PrimaryButtonTypography(
                fontFamily = localTypography.fontFamily
                    ?: style.typography.fontFamily?.let { fontFamily ->
                        FontFamily(Font(fontFamily))
                    },
                fontSize = if (localTypography.fontSize.isUnspecified) {
                    style.typography.fontSize
                } else {
                    localTypography.fontSize
                }
            )
        }
    }
}

@Composable
internal fun PrimaryButtonTheme(
    colors: PrimaryButtonColors = PrimaryButtonColors(),
    shape: PrimaryButtonShape = PrimaryButtonShape(),
    typography: PrimaryButtonTypography = PrimaryButtonTypography(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalPrimaryButtonColors provides colors,
        LocalPrimaryButtonShape provides shape,
        LocalPrimaryButtonTypography provides typography
    ) {
        content()
    }
}
