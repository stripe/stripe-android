package com.stripe.android.financialconnections.features.accountpicker

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun AccountPickerScreen() {
    val viewModel: AccountPickerViewModel = mavericksViewModel()
    val state: State<AccountPickerState> = viewModel.collectAsState()
    Column {
        Text(
            state.value.title,
            style = FinancialConnectionsTheme.typography.heading
        )
    }
}
