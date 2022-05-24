package com.stripe.android.link.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.PaymentsThemeDefaults

private val LinkTeal = Color(0xFF33DDB3)
private val ActionGreen = Color(0xFF05A87F)
private val ButtonLabel = Color(0xFF1D3944)

private val LightComponentBackground = Color.White
private val LightComponentBorder = Color(0xFFE0E6EB)
private val LightComponentDivider = Color(0xFFEFF2F4)
private val LightTextPrimary = Color(0xFF30313D)
private val LightTextSecondary = Color(0xFF6A7383)
private val LightTextDisabled = Color(0xFFA3ACBA)
private val LightBackground = Color.White
private val LightFill = Color(0xFFF6F8FA)
private val LightCloseButton = Color(0xFF30313D)
private val LightLinkLogo = Color(0xFF1D3944)
private val LightSecondaryButtonLabel = Color(0xFF1D3944)

private val DarkComponentBackground = Color(0x2E747480)
private val DarkComponentBorder = Color(0x5C787880)
private val DarkComponentDivider = Color(0x33787880)
private val DarkTextPrimary = Color.White
private val DarkTextSecondary = Color(0x99EBEBF5)
private val DarkTextDisabled = Color(0x61FFFFFF)
private val DarkBackground = Color(0xFF1C1C1E)
private val DarkFill = Color(0x33787880)
private val DarkCloseButton = Color(0x99EBEBF5)
private val DarkLinkLogo = Color.White
private val DarkSecondaryButtonLabel = ActionGreen

internal data class LinkColors(
    val componentBackground: Color,
    val componentBorder: Color,
    val componentDivider: Color,
    val buttonLabel: Color,
    val disabledText: Color,
    val closeButton: Color,
    val linkLogo: Color,
    val secondaryButtonLabel: Color,
    val materialColors: Colors
)

@Composable
internal fun PaymentsThemeForLink(
    content: @Composable () -> Unit
) {
    val paymentsTheme = PaymentsThemeDefaults.colors(isSystemInDarkTheme())

    PaymentsTheme(
        colors = paymentsTheme.copy(
            materialColors = paymentsTheme.materialColors.copy(
                primary = ActionGreen
            )
        )
    ) {
        content()
    }
}

@Composable
internal fun linkTextFieldColors() =
    TextFieldDefaults.textFieldColors(
        backgroundColor = MaterialTheme.colors.background,
        cursorColor = LinkTeal,
        focusedLabelColor = LinkTeal,
        focusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )

internal object LinkThemeConfig {
    fun colors(isDark: Boolean): LinkColors {
        return if (isDark) colorsDark else colorsLight
    }

    private val colorsLight = LinkColors(
        componentBackground = LightComponentBackground,
        componentBorder = LightComponentBorder,
        componentDivider = LightComponentDivider,
        buttonLabel = ButtonLabel,
        disabledText = LightTextDisabled,
        closeButton = LightCloseButton,
        linkLogo = LightLinkLogo,
        secondaryButtonLabel = LightSecondaryButtonLabel,
        materialColors = lightColors(
            primary = LinkTeal,
            secondary = LightFill,
            background = LightBackground,
            surface = LightBackground,
            onPrimary = LightTextPrimary,
            onSecondary = LightTextSecondary
        )
    )

    private val colorsDark = LinkColors(
        componentBackground = DarkComponentBackground,
        componentBorder = DarkComponentBorder,
        componentDivider = DarkComponentDivider,
        buttonLabel = ButtonLabel,
        disabledText = DarkTextDisabled,
        closeButton = DarkCloseButton,
        linkLogo = DarkLinkLogo,
        secondaryButtonLabel = DarkSecondaryButtonLabel,
        materialColors = darkColors(
            primary = LinkTeal,
            secondary = DarkFill,
            background = DarkBackground,
            surface = DarkBackground,
            onPrimary = DarkTextPrimary,
            onSecondary = DarkTextSecondary
        )
    )
}
