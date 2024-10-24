package com.stripe.android.connect.example.ui.settings

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
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.connect.example.MainContent
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.data.OnboardingSettings
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
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        actions = {
            TextButton(
                enabled = state.saveEnabled,
                onClick = {
                    viewModel.saveSettings()
                    onDismiss()
                },
            ) {
                Text(text = stringResource(R.string.save))
            }
        }
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { SelectAnAccount(
                accounts = state.accounts,
                selectedAccountId = state.selectedAccountId,
                onAccountSelected = viewModel::onAccountSelected,
                onOtherAccountInputChanged = viewModel::onOtherAccountInputChanged,
            ) }
            item { ComponentSettings(
                onboardingSettings = state.onboardingSettings,
                onSettingsChanged = viewModel::onOnboardingSettingsChanged
            ) }
            item { ApiServerSettings(
                serverUrl = state.serverUrl,
                onServerUrlChanged = viewModel::onServerUrlChanged,
                resetServerUrlEnabled = state.serverUrlResetEnabled,
                resetServerUrlClicked = viewModel::onResetServerUrlClicked,
            ) }
        }
    }
}

@Composable
private fun SelectAnAccount(
    accounts: List<DemoMerchant>,
    selectedAccountId: String?,
    onAccountSelected: (String) -> Unit,
    onOtherAccountInputChanged: (String) -> Unit,
) {
    Text("Select a demo account", style = MaterialTheme.typography.h6)
    Spacer(modifier = Modifier.height(8.dp))
    accounts.forEach { merchant ->
        when (merchant) {
            is DemoMerchant.Merchant -> {
                Row {
                    Column {
                        Text(text = merchant.displayName)
                        Text(text = merchant.merchantId, style = MaterialTheme.typography.caption)
                    }
                    RadioButton(
                        selected = merchant.merchantId == selectedAccountId,
                        onClick = { onAccountSelected(merchant.merchantId) },
                    )
                }
            }
            is DemoMerchant.Other -> {
                OutlinedTextField(
                    value = merchant.merchantId ?: "",
                    onValueChange = onOtherAccountInputChanged,
                    label = { Text(stringResource(R.string.other)) },
                    placeholder = { Text(stringResource(R.string.account_id_placeholder)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ComponentSettings(
    onboardingSettings: OnboardingSettings,
    onSettingsChanged: (OnboardingSettings) -> Unit,
) {
    Text("Component Settings", style = MaterialTheme.typography.h6)
    Spacer(modifier = Modifier.height(8.dp))
    NavigationLink(
        onClick = { /* Navigate to OnboardingSettingsView */ },
    ) {
        Text(stringResource(R.string.account_onboarding))
    }

    NavigationLink(
        onClick = { /* Navigate to PresentationSettingsView */ },
    ) {
        Text(stringResource(R.string.presentation_options))
    }
}

@Composable
private fun ApiServerSettings(
    serverUrl: String,
    onServerUrlChanged: (String) -> Unit,
    resetServerUrlEnabled: Boolean,
    resetServerUrlClicked: () -> Unit,
) {
    Text("API Server Settings", style = MaterialTheme.typography.h6)
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = serverUrl,
        onValueChange = onServerUrlChanged,
        label = { Text("Server URL") },
        placeholder = { Text("https://example.com") },
        modifier = Modifier.fillMaxWidth()
    )
    Button(
        enabled = resetServerUrlEnabled,
        onClick = resetServerUrlClicked,
    ) {
        Text("Reset to default")
    }
}

@Composable
private fun NavigationLink(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
            Icon(
                imageVector = Icons.AutoMirrored.Default.ArrowForward,
                contentDescription = null
            )
        }
    }
}
