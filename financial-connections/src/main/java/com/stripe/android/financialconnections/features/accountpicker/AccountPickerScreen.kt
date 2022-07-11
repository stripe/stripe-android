package com.stripe.android.financialconnections.features.accountpicker

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
fun AccountPickerScreen() {
    Text(text = "ACCOUNT PICKER", style = FinancialConnectionsTheme.typography.subtitle)
}