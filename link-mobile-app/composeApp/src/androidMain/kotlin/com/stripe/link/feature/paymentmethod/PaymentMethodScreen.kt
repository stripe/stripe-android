package com.stripe.link.feature.paymentmethod

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.link.core.ui.component.LinkSwitch
import com.stripe.link.core.ui.component.ListItem
import com.stripe.link.core.ui.component.ListItemConfig
import com.stripe.link.core.ui.component.ListItemEnablement

@Composable
fun PaymentMethodScreen(
    groups: List<PaymentMethodActionGroup>,
    isActionEnabled: (PaymentMethodAction) -> Boolean,
    onAlertTapHapticFeedback: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        groups.forEach { group ->
            PaymentMethodActionSection(
                group = group,
                isActionEnabled = isActionEnabled,
                onAlertTapHapticFeedback = onAlertTapHapticFeedback,
            )
        }
    }
}

@Composable
private fun PaymentMethodActionSection(
    group: PaymentMethodActionGroup,
    isActionEnabled: (PaymentMethodAction) -> Boolean,
    onAlertTapHapticFeedback: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        section(group.actions) { action ->
            val effectiveSwitchState = action.switchState

            if (effectiveSwitchState != null) {
                // Toggle row — the row itself is not tappable; only the switch responds.
                ListItem(
                    label = action.title,
                    detailsContent = {
                        LinkSwitch(
                            checked = effectiveSwitchState.checked,
                            onCheckedChange = action.onToggle,
                            enabled = effectiveSwitchState.enabled,
                        )
                    },
                    config = ListItemConfig(),
                )
            } else {
                // Regular tappable row.
                ListItem(
                    label = action.title,
                    config = ListItemConfig(),
                    enabledState = if (isActionEnabled(action)) {
                        ListItemEnablement.Enabled
                    } else {
                        ListItemEnablement.Disabled
                    },
                    onClick = {
                        if (action.triggersAlertHapticStack) onAlertTapHapticFeedback()
                        action.onClick()
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

@Composable
private fun section(
    actions: List<PaymentMethodAction>,
    content: @Composable (PaymentMethodAction) -> Unit,
) {
    actions.forEach { action ->
        content(action)
    }
}

// ---------------------------------------------------------------------------
// Domain model
// ---------------------------------------------------------------------------

data class PaymentMethodActionGroup(val actions: List<PaymentMethodAction>)
