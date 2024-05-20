package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun PaymentMethodRowButton(
    isEnabled: Boolean,
    isSelected: Boolean,
    iconContent: @Composable RowScope.() -> Unit,
    textContent: @Composable RowScope.() -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    RowButton(
        isEnabled = isEnabled,
        isSelected = isSelected,
        onClick = onClick,
        contentPaddingValues = PaddingValues(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            iconContent()

            textContent()

            if (trailingContent != null) {
                Spacer(modifier = Modifier.weight(1f))
                trailingContent()
            }
        }
    }
}
