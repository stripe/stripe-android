package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.toComposeShapes
import com.stripe.android.uicore.toComposeTypography

private val defaultMaterialColors = StripeThemeDefaults.colorsLight.materialColors

private val defaultTextColor = Color(0xFF6A7383)

private val colors = defaultMaterialColors.copy(
    secondary = defaultMaterialColors.background,
    secondaryVariant = Color(0x1F6A7383),
    onSecondary = defaultMaterialColors.onSecondary,
    surface = Color(0xFFF6F8FA),
    onSurface = defaultTextColor,
    background = defaultMaterialColors.background,
    onBackground = defaultTextColor
)

@Composable
internal fun BacsMandateConfirmationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = colors,
        typography = StripeThemeDefaults.typography.toComposeTypography(),
        shapes = StripeThemeDefaults.shapes.toComposeShapes().material
    ) {
        content()
    }
}
