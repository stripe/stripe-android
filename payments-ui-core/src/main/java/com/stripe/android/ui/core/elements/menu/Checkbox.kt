package com.stripe.android.ui.core.elements.menu

import androidx.annotation.RestrictTo
import androidx.compose.material.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.ui.core.PaymentsTheme

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val checkboxColors = CheckboxDefaults.colors(
        checkedColor = PaymentsTheme.colors.material.primary,
        uncheckedColor = PaymentsTheme.colors.subtitle,
        checkmarkColor = PaymentsTheme.colors.material.surface
    )

    androidx.compose.material.Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = checkboxColors
    )
}
