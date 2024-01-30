package com.stripe.android.financialconnections.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Colors
import com.stripe.android.financialconnections.ui.theme.Neutral900

@Composable
internal fun FinancialConnectionsModalBottomSheetLayout(
    sheetContent: @Composable ColumnScope.() -> Unit,
    sheetState: ModalBottomSheetState,
    content: @Composable () -> Unit
) {
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetBackgroundColor = v3Colors.backgroundSurface,
        sheetShape =  RoundedCornerShape(20.dp, 20.dp, 0.dp, 0.dp),
        scrimColor = Neutral900.copy(alpha = 0.32f),
        sheetContent = sheetContent,
        content = content
    )
}
