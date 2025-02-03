package com.stripe.android.financialconnections.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun FinancialConnectionsScaffold(
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        backgroundColor = FinancialConnectionsTheme.colors.background,
        contentColor = FinancialConnectionsTheme.colors.textDefault,
        topBar = topBar,
        content = content
    )
}
