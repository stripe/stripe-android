package com.stripe.android.connect.example.ui.settings

import android.accounts.Account
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.connect.example.MainContent
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.ui.settings.SettingsViewModel.SettingsState.DemoMerchant

@Composable
fun SettingsView(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    MainContent(
        title = stringResource(R.string.settings),
        navigationIcon = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        actions = {
            TextButton(
                enabled = state.saveEnabled,
                onClick = {
                    viewModel.saveSettings()
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.save))
            }
        }
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp)
        ) {

        }
    }
}

@Composable
private fun LazyListScope.SelectAnAccount(
    merchants: List<DemoMerchant>,
    onMerchantSelected: () -> DemoMerchant,
) {

}

@Composable
private fun LazyListScope.ComponentSettings() {

}

@Composable
private fun LazyListScope.ApiServerSettings() {

}
