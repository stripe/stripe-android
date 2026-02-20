package com.stripe.android.common.taptoadd.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stripe.android.link.ui.inline.LocalLinkInlineSignupBackgroundColor
import com.stripe.android.paymentsheet.ui.PrimaryButtonColors
import com.stripe.android.paymentsheet.ui.PrimaryButtonShape
import com.stripe.android.paymentsheet.ui.PrimaryButtonTheme
import com.stripe.android.uicore.StripeColors
import com.stripe.android.uicore.StripeShapes
import com.stripe.android.uicore.StripeTheme
import com.stripe.stripeterminal.external.models.TapToPayUxConfiguration
import android.graphics.Color as BaseAndroidColor

@Composable
internal fun TapToAddTheme(
    content: @Composable () -> Unit,
) {
    StripeTheme(
        colors = TapToAddThemeDefaults.colors,
        shapes = TapToAddThemeDefaults.shapes,
    ) {
        PrimaryButtonTheme(
            colors = TapToAddThemeDefaults.primaryButtonColors,
            shape = TapToAddThemeDefaults.primaryButtonShape,
        ) {
            CompositionLocalProvider(
                LocalLinkInlineSignupBackgroundColor provides MaterialTheme.colors.background
            ) {
                content()
            }
        }
    }
}

private object TapToAddThemeDefaults {
    private val componentBorderColor = Color(0xFF48484A)

    val primaryButtonShape = PrimaryButtonShape(
        cornerRadius = 12.dp,
        borderStrokeWidth = 0.dp,
        height = 48.dp,
    )

    val primaryButtonColors = PrimaryButtonColors(
        background = Color.White,
        onBackground = Color.Black,
    )

    val shapes = StripeShapes(
        cornerRadius = 12.0f,
        bottomSheetCornerRadius = 6.0f,
        borderStrokeWidth = 1.0f,
    )

    val colors = StripeColors(
        component = Color(0xFF1C1C1E),
        componentBorder = componentBorderColor,
        componentDivider = componentBorderColor,
        onComponent = Color.White,
        subtitle = Color(0xFF8D8D93),
        textCursor = Color.White,
        placeholderText = Color(0xFF95959C),
        appBarIcon = Color.White,
        materialColors = darkColors(
            primary = Color.White,
            primaryVariant = Color(0xFF7A7A7A),
            background = Color.Black,
            onBackground = Color(0xFFE3E3E3),
            surface = Color.Black,
            onSurface = Color(0xFFE3E3E3),
            error = Color.Red,
        ),
    )
}

internal fun createTapToAddUxConfiguration(): TapToPayUxConfiguration {
    return TapToPayUxConfiguration.Builder()
        .darkMode(darkMode = TapToPayUxConfiguration.DarkMode.DARK)
        .colors(
            colors = TapToPayUxConfiguration.ColorScheme.Builder()
                .primary(
                    primary = TapToPayUxConfiguration.Color.Value(
                        color = BaseAndroidColor.BLACK
                    )
                )
                .build()
        )
        .build()
}
