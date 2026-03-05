package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.stripe.android.uicore.StripeColors
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.stripeterminal.external.models.TapToPayUxConfiguration
import android.graphics.Color as BaseAndroidColor

@Composable
internal fun TapToAddTheme(
    content: @Composable () -> Unit,
) {
    StripeTheme(
        colors = TapToAddThemeDefaults.colors,
        typography = TapToAddThemeDefaults.typography,
    ) {
        content()
    }
}

private object TapToAddThemeDefaults {
    val typography = StripeThemeDefaults.typography.copy(
        h4 = TextStyle(
            fontSize = 28.sp,
            letterSpacing = (-0.48).sp,
            lineHeight = 37.44.sp,
            fontWeight = FontWeight.W500,
        )
    )

    val colors: StripeColors
        @Composable
        get() {
            val isDarkTheme = isSystemInDarkTheme()

            return remember {
                val colors = StripeThemeDefaults.colors(isDarkTheme)

                colors.copy(
                    materialColors = colors.materialColors.copy(
                        primaryVariant = if (isDarkTheme) {
                            Color(0xFF808080)
                        } else {
                            Color(0xFF757F8F)
                        },
                        secondaryVariant = if (isDarkTheme) {
                            Color(0xFFE3E3E3)
                        } else {
                            Color(0xFF757F8F)
                        },
                        error = Color(0xFFF30000),
                    ),
                )
            }
        }
}

internal fun createTapToAddUxConfiguration(): TapToPayUxConfiguration {
    return TapToPayUxConfiguration.Builder()
        .darkMode(darkMode = TapToPayUxConfiguration.DarkMode.SYSTEM)
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
