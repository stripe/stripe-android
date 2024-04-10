package com.stripe.android.financialconnections.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.navigation.bottomsheet.BottomSheetNavigator
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.Neutral900

@Composable
internal fun FinancialConnectionsModalBottomSheetLayout(
    bottomSheetNavigator: BottomSheetNavigator,
    content: @Composable () -> Unit
) {
    com.stripe.android.financialconnections.navigation.bottomsheet.ModalBottomSheetLayout(
        bottomSheetNavigator = bottomSheetNavigator,
        sheetBackgroundColor = colors.backgroundSurface,
        sheetShape = RoundedCornerShape(20.dp, 20.dp, 0.dp, 0.dp),
        scrimColor = Neutral900.copy(alpha = 0.32f),
        content = content,
    )
}
