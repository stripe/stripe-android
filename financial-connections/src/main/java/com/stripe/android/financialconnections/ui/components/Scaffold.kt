package com.stripe.android.financialconnections.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun FinancialConnectionsScaffold(
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = FinancialConnectionsTheme.colors.background,
        contentColor = FinancialConnectionsTheme.colors.textDefault,
        topBar = topBar,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        content = content
    )
}
