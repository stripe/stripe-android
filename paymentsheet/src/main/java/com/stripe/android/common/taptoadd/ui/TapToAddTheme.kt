package com.stripe.android.common.taptoadd.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.stripe.android.common.taptoadd.LocalTapToAddImageRepository
import com.stripe.android.common.taptoadd.TapToAddImageRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ui.PaymentElementTheme
import com.stripe.android.paymentsheet.ui.isDarkTheme
import com.stripe.android.uicore.stripeThemeIsDark
import com.stripe.stripeterminal.external.models.TapToPayUxConfiguration

@Composable
internal fun TapToAddTheme(
    appearance: PaymentSheet.Appearance,
    imageRepository: TapToAddImageRepository?,
    content: @Composable () -> Unit,
) {
    PaymentElementTheme(appearance = appearance) {
        val isDark = MaterialTheme.stripeThemeIsDark
        MaterialTheme(
            colors = MaterialTheme.colors.copy(
                background = MaterialTheme.colors.surface,
                primaryVariant = if (isDark) {
                    PRIMARY_VARIANT_DARK
                } else {
                    VARIANT_LIGHT
                },
                secondaryVariant = if (isDark) {
                    SECONDARY_VARIANT_DARK
                } else {
                    VARIANT_LIGHT
                },
                error = ERROR_COLOR,
            ),
            typography = MaterialTheme.typography.copy(
                h4 = MaterialTheme.typography.h4.merge(TapToAddThemeDefaults.h4),
            ),
        ) {
            CompositionLocalProvider(
                LocalTapToAddImageRepository provides imageRepository
            ) {
                content()
            }
        }
    }
}

private val PRIMARY_VARIANT_DARK = Color(0xFF808080)
private val SECONDARY_VARIANT_DARK = Color(0xFFE3E3E3)
private val VARIANT_LIGHT = Color(0xFF757F8F)
private val ERROR_COLOR = Color(0xFFF30000)

private object TapToAddThemeDefaults {
    val h4 = TextStyle(
        fontSize = 28.sp,
        letterSpacing = (-0.48).sp,
        lineHeight = 37.44.sp,
        fontWeight = FontWeight.W500,
    )
}

internal fun createTapToAddUxConfiguration(
    appearance: PaymentSheet.Appearance,
    isSystemDark: Boolean,
): TapToPayUxConfiguration {
    val isDark = appearance.themeMode.isDarkTheme(isSystemDark)

    return TapToPayUxConfiguration.Builder()
        .darkMode(
            darkMode = if (isDark) {
                TapToPayUxConfiguration.DarkMode.DARK
            } else {
                TapToPayUxConfiguration.DarkMode.LIGHT
            }
        )
        .colors(
            colors = TapToPayUxConfiguration.ColorScheme.Builder()
                .primary(
                    primary = TapToPayUxConfiguration.Color.Value(
                        color = appearance.getColors(isDark).primary,
                    )
                )
                .build()
        )
        .build()
}
