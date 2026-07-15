package com.stripe.link.core.ui.component

import androidx.compose.material.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A styled toggle switch that matches the Link design system.
 *
 * @param checked Whether the switch is in the on state.
 * @param onCheckedChange Callback when the switch is toggled. Pass `null` to disable interaction.
 * @param modifier [Modifier] applied to the switch.
 * @param enabled Whether the switch is interactive.
 */
@Composable
fun LinkSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled && onCheckedChange != null,
    )
}
