package com.stripe.android.financialconnections.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.navigation.bottomsheet.BottomSheetNavigator
import com.stripe.android.financialconnections.navigation.bottomsheet.ModalBottomSheetLayout
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetLayout
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetLayoutInfo
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetState
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetLayoutInfo

@Composable
internal fun FinancialConnectionsBottomSheetLayout(
    state: StripeBottomSheetState,
    modifier: Modifier = Modifier,
    onDismissed: () -> Unit,
    content: @Composable () -> Unit,
) {
    val layoutInfo = rememberFinancialConnectionsBottomSheetLayoutInfo()

    StripeBottomSheetLayout(
        state = state,
        layoutInfo = layoutInfo,
        modifier = modifier,
        onDismissed = onDismissed,
        sheetContent = content,
    )
}

@Composable
internal fun FinancialConnectionsModalBottomSheetLayout(
    bottomSheetNavigator: BottomSheetNavigator,
    content: @Composable () -> Unit
) {
    val layoutInfo = rememberFinancialConnectionsBottomSheetLayoutInfo()

    ModalBottomSheetLayout(
        bottomSheetNavigator = bottomSheetNavigator,
        sheetBackgroundColor = layoutInfo.sheetBackgroundColor,
        sheetShape = layoutInfo.sheetShape,
        scrimColor = layoutInfo.scrimColor,
        content = content,
    )
}

@Composable
private fun rememberFinancialConnectionsBottomSheetLayoutInfo(): StripeBottomSheetLayoutInfo {
    val scrimAlpha = if (isSystemInDarkTheme()) 0.48f else 0.32f

    return rememberStripeBottomSheetLayoutInfo(
        cornerRadius = 20.dp,
        sheetBackgroundColor = colors.background,
        scrimColor = Color.Black.copy(alpha = scrimAlpha),
    )
}
