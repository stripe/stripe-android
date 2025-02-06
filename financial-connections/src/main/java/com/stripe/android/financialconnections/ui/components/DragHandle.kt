package com.stripe.android.financialconnections.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

private val DragHandleSize = DpSize(width = 32.dp, height = 4.dp)
private val DragHandleCornerRadius = 40.dp

@Composable
internal fun DragHandle(
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(DragHandleSize)
                .background(
                    color = FinancialConnectionsTheme.colors.spinnerNeutral,
                    shape = RoundedCornerShape(DragHandleCornerRadius),
                )
        )
    }
}
