package com.stripe.android.financialconnections.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.Neutral900
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetLayout
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetState
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetLayoutInfo

@Composable
internal fun FinancialConnectionsBottomSheetLayout(
    state: StripeBottomSheetState,
    modifier: Modifier = Modifier,
    onDismissed: () -> Unit,
    content: @Composable () -> Unit,
) {
    val layoutInfo = rememberStripeBottomSheetLayoutInfo(
        cornerRadius = 20.dp,
        sheetBackgroundColor = FinancialConnectionsTheme.colors.backgroundSurface,
        scrimColor = Neutral900.copy(alpha = 0.32f),
    )

    StripeBottomSheetLayout(
        state = state,
        layoutInfo = layoutInfo,
        onDismissed = onDismissed,
        sheetContent = content,
    )
}
