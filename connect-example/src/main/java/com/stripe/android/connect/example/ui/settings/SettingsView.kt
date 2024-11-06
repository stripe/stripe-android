package com.stripe.android.connect.example.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.connect.example.MainContent
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.ui.settings.SettingsViewModel.SettingsState.DemoMerchant

@Composable
fun SettingsView(
    onDismiss: () -> Unit,
    onReloadRequested: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var serverUrlDidChange = remember { false }

    BackHandler { onDismiss() }
    LaunchedEffect(state.serverUrl) { serverUrlDidChange = true } // track if the serverURL ever changes
    MainContent(
        title = stringResource(R.string.settings),
        navigationIcon = {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = stringResource(R.string.cancel)
                )
            }
        },
        actions = {
            IconButton(
                enabled = state.saveEnabled,
                onClick = {
                    viewModel.saveSettings()
                    onDismiss()
                    if (serverUrlDidChange) onReloadRequested() // reload if the serverURL changed
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.save)
                )
            }
        }
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SelectAnAccount(
                    accounts = state.accounts,
                    selectedAccount = state.selectedAccount,
                    onAccountSelected = viewModel::onAccountSelected,
                    onOtherAccountInputChanged = viewModel::onOtherAccountInputChanged,
                )
            }
            item {
                ApiServerSettings(
                    serverUrl = state.serverUrl,
                    onServerUrlChanged = viewModel::onServerUrlChanged,
                    resetServerUrlEnabled = state.serverUrlResetEnabled,
                    resetServerUrlClicked = viewModel::onResetServerUrlClicked,
                )
            }
        }
    }
}

@Composable
private fun SelectAnAccount(
    accounts: List<DemoMerchant>,
    selectedAccount: DemoMerchant?,
    onAccountSelected: (DemoMerchant) -> Unit,
    onOtherAccountInputChanged: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.select_demo_account), style = MaterialTheme.typography.h6)
        accounts.forEach { merchant ->
            when (merchant) {
                is DemoMerchant.Merchant -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAccountSelected(merchant) },
                    ) {
                        RadioButton(
                            selected = merchant.merchantId == selectedAccount?.merchantId,
                            onClick = null, // onClick handled by row
                        )
                        Column {
                            Text(text = merchant.displayName)
                            Text(text = merchant.merchantId, style = MaterialTheme.typography.caption)
                        }
                    }
                }
                is DemoMerchant.Other -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAccountSelected(merchant) },
                    ) {
                        RadioButton(
                            selected = merchant.merchantId == selectedAccount?.merchantId,
                            onClick = null, // onClick handled by row
                        )
                        OutlinedTextField(
                            value = merchant.merchantId,
                            onValueChange = onOtherAccountInputChanged,
                            label = { Text(stringResource(R.string.other)) },
                            placeholder = { Text(stringResource(R.string.account_id_placeholder)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiServerSettings(
    serverUrl: String,
    onServerUrlChanged: (String) -> Unit,
    resetServerUrlEnabled: Boolean,
    resetServerUrlClicked: () -> Unit,
) {
    Text(stringResource(R.string.api_server_settings), style = MaterialTheme.typography.h6)
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = serverUrl,
        onValueChange = onServerUrlChanged,
        label = { Text(stringResource(R.string.server_url_label)) },
        placeholder = { Text(stringResource(R.string.server_url_placeholder)) },
        modifier = Modifier.fillMaxWidth()
    )
    Button(
        enabled = resetServerUrlEnabled,
        onClick = resetServerUrlClicked,
    ) {
        Text(stringResource(R.string.reset_to_default))
    }
}
