package com.stripe.android.uicore.elements.menu

import androidx.annotation.RestrictTo
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.uicore.stripeColors

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val checkboxColors = CheckboxDefaults.colors(
        checkedColor = MaterialTheme.colors.primary,
        uncheckedColor = MaterialTheme.stripeColors.subtitle,
        checkmarkColor = MaterialTheme.colors.surface
    )

    androidx.compose.material.Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = checkboxColors
    )
}
