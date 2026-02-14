package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.getBorderStroke
import com.stripe.android.uicore.stripeColorScheme

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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.stripeColorScheme.component
        ),
        border = MaterialTheme.getBorderStroke(isSelected),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 1.5.dp else 0.dp)
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
