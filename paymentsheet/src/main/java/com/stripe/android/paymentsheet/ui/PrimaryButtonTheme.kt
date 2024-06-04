package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.takeOrElse
import androidx.core.content.ContextCompat
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

        return remember(
            style,
            context,
            localColors,
            isDarkTheme
        ) {
            PrimaryButtonColors(
                background = localColors.background.takeOrElse {
                    Color(style.getBackgroundColor(context))
                },
                onBackground = localColors.onBackground.takeOrElse {
                    Color(style.getOnBackgroundColor(context))
                },
                successBackground = localColors.successBackground.takeOrElse {
                    Color(
                        ContextCompat.getColor(
                            context,
                            R.color.stripe_paymentsheet_primary_button_success_background
                        )
                    )
                },
                onSuccessBackground = localColors.onSuccessBackground.takeOrElse {
                    if (isDarkTheme) {
                        Color.Black
                    } else {
                        Color.White
                    }
                },
                border = localColors.border.takeOrElse {
                    Color(style.getBorderStrokeColor(context))
                },
            )
        }
    }

    @Composable
    private fun getPrimaryButtonShape(): PrimaryButtonShape {
        val style = StripeTheme.primaryButtonStyle
        val localShape = LocalPrimaryButtonShape.current

        return remember(style, localShape) {
            PrimaryButtonShape(
                cornerRadius = localShape.cornerRadius.takeOrElse {
                    style.shape.cornerRadius.dp
                },
                borderStrokeWidth = localShape.borderStrokeWidth.takeOrElse {
                    style.shape.borderStrokeWidth.dp
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
                fontSize = localTypography.fontSize.takeOrElse {
                    style.typography.fontSize
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
