package com.stripe.android.uicore.elements.menu

import androidx.annotation.RestrictTo
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.uicore.stripeColorScheme

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val checkboxColors = CheckboxDefaults.colors(
        checkedColor = MaterialTheme.colorScheme.primary,
        uncheckedColor = MaterialTheme.stripeColorScheme.subtitle,
        checkmarkColor = MaterialTheme.colorScheme.surface
    )

    androidx.compose.material3.Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = checkboxColors
    )
}
