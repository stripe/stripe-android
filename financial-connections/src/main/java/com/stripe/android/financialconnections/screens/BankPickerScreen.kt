package com.stripe.android.financialconnections.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.airbnb.mvrx.compose.collectAsState

@Composable
internal fun BankPickerScreen(
    viewModel: BankPickerViewModel,
    navController: NavHostController
) {
    val state by viewModel.collectAsState()

    Column {
        Spacer(modifier = Modifier.height(16.dp))
        Text("Bank Picker ${state.test}")
        Spacer(modifier = Modifier.height(16.dp))
    }
}