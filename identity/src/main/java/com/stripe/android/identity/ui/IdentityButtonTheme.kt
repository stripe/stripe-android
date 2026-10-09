package com.stripe.android.identity.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.identity.IdentityVerificationSheet

internal data class IdentityButtonStyle(
    val colors: ButtonColors,
    val shape: Shape,
    val height: Dp?,
    val elevation: ButtonElevation?,
    val elevationDp: Dp?,
    val uppercase: Boolean?,
    val border: BorderStroke?
) {
    fun text(text: String, uppercase: Boolean): String {
        return if (this.uppercase ?: uppercase) text.uppercase() else text
    }
}

internal object IdentityButtonTheme {
    val primary: IdentityButtonStyle
        @Composable
        get() {
            val style = LocalIdentityPrimaryButtonStyle.current
            return IdentityButtonStyle(
                colors = identityButtonColors(
                    backgroundColor = style?.backgroundColor,
                    textColor = style?.textColor,
                    defaultColors = ButtonDefaults.buttonColors()
                ),
                shape = style?.shape?.let {
                    RoundedCornerShape(it.cornerRadiusDp.dp)
                } ?: MaterialTheme.shapes.small,
                height = style?.shape?.heightDp?.dp,
                elevation = style?.elevationDp?.let {
                    fixedButtonElevation(it)
                } ?: ButtonDefaults.elevation(),
                elevationDp = style?.elevationDp?.dp,
                uppercase = style?.uppercase,
                border = null
            )
        }

    val secondary: IdentityButtonStyle
        @Composable
        get() {
            val style = LocalIdentitySecondaryButtonStyle.current
            return IdentityButtonStyle(
                colors = identityButtonColors(
                    backgroundColor = style?.backgroundColor,
                    textColor = style?.textColor,
                    defaultColors = ButtonDefaults.outlinedButtonColors()
                ),
                shape = style?.shape?.let {
                    RoundedCornerShape(it.cornerRadiusDp.dp)
                } ?: MaterialTheme.shapes.small,
                height = style?.shape?.heightDp?.dp,
                elevation = style?.elevationDp?.let { fixedButtonElevation(it) },
                elevationDp = style?.elevationDp?.dp,
                uppercase = style?.uppercase,
                border = if (style?.showBorder == false) {
                    null
                } else {
                    ButtonDefaults.outlinedBorder
                }
            )
        }
}

@Composable
private fun fixedButtonElevation(elevationDp: Float): ButtonElevation {
    val elevation = elevationDp.dp
    return ButtonDefaults.elevation(
        defaultElevation = elevation,
        pressedElevation = elevation,
        disabledElevation = elevation,
        hoveredElevation = elevation,
        focusedElevation = elevation
    )
}

@Composable
private fun identityButtonColors(
    backgroundColor: IdentityVerificationSheet.Configuration.ButtonColor?,
    textColor: IdentityVerificationSheet.Configuration.ButtonColor?,
    defaultColors: ButtonColors
): ButtonColors {
    val isDark = isSystemInDarkTheme()
    val resolvedBackgroundColor = backgroundColor.resolve(isDark)
        ?: defaultColors.backgroundColor(enabled = true).value
    val resolvedTextColor = textColor.resolve(isDark)
        ?: defaultColors.contentColor(enabled = true).value
    return ButtonDefaults.buttonColors(
        backgroundColor = resolvedBackgroundColor,
        contentColor = resolvedTextColor,
        disabledBackgroundColor = defaultColors.backgroundColor(enabled = false).value,
        disabledContentColor = defaultColors.contentColor(enabled = false).value
    )
}

private fun IdentityVerificationSheet.Configuration.ButtonColor?.resolve(isDark: Boolean): Color? {
    val color = if (isDark) this?.dark else this?.light
    return color?.let(::Color)
}

internal val LocalIdentityPrimaryButtonStyle = staticCompositionLocalOf<
    IdentityVerificationSheet.Configuration.PrimaryButtonStyle?
> { null }

internal val LocalIdentitySecondaryButtonStyle = staticCompositionLocalOf<
    IdentityVerificationSheet.Configuration.SecondaryButtonStyle?
> { null }
