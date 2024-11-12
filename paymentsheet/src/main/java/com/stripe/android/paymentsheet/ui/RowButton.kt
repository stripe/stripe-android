package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.getBorderStroke
import com.stripe.android.uicore.stripeColors

@Composable
internal fun RowButton(
    isEnabled: Boolean,
    isSelected: Boolean,
    isClickable: Boolean = isEnabled,
    onClick: () -> Unit,
    contentPaddingValues: PaddingValues,
    verticalArrangement: Arrangement.Vertical,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .alpha(alpha = if (isEnabled) 1.0F else 0.6F),
        shape = MaterialTheme.shapes.medium,
        backgroundColor = MaterialTheme.stripeColors.component,
        border = MaterialTheme.getBorderStroke(isSelected),
        elevation = if (isSelected) 1.5.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .selectable(
                    selected = isSelected,
                    enabled = isClickable,
                    onClick = onClick
                )
                .padding(contentPaddingValues),
            verticalArrangement = verticalArrangement,
        ) {
            content()
        }
    }
}
