package com.stripe.android.connect.example.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.data.Merchant
import com.stripe.android.connect.example.data.SettingsService
import java.net.URL

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SettingsView(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
//    val state by viewModel.state.collectAsState()
//    val navController = rememberNavController()
//
//    val isUsingCustomMerchant = state.selectedAccount?.let { selectedAccount ->
//        state.accounts?.contains(selectedAccount) != true
//    } ?: true
//
//    val isMerchantIdValid = if (!isUsingCustomMerchant) {
//        true
//    } else {
//        state.selectedAccount?.merchantId?.let { id ->
//            id.startsWith("acct_") && id.length > 5
//        } ?: false
//    }
//
//    val saveEnabled = isCustomEndpointValid && isMerchantIdValid
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text(stringResource(R.string.settings_title)) },
//                navigationIcon = {
//                    IconButton(onClick = onDismiss) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cancel))
//                    }
//                },
//                actions = {
//                    TextButton(
//                        onClick = {
//                            viewModel.saveSettings()
//                            onDismiss()
//                        },
//                        enabled = saveEnabled
//                    ) {
//                        Text(stringResource(R.string.save))
//                    }
//                }
//            )
//        }
//    ) { paddingValues ->
//        LazyColumn(
//            modifier = Modifier.padding(paddingValues),
//            contentPadding = PaddingValues(16.dp)
//        ) {
//            item {
//                Text(stringResource(R.string.select_demo_account), style = MaterialTheme.typography.h6)
//                Spacer(modifier = Modifier.height(8.dp))
//            }
//
//            items(items = state.accounts ?: emptyList()) { merchant ->
//                ListItem(
//                    text = { Text(merchant.displayName) },
//                    secondaryText = { Text(merchant.merchantId) },
//                    trailing = {
//                        if (state.selectedAccount?.merchantId == merchant.merchantId) {
//                            Icon(
//                                imageVector = Icons.Default.Check,
//                                contentDescription = stringResource(R.string.selected),
//                                tint = MaterialTheme.colors.primary
//                            )
//                        }
//                    },
//                    modifier = Modifier.clickable { viewModel.onAccountSelected(merchant) }
//                )
//            }
//
//            item {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    val customMerchantId = if (isUsingCustomMerchant) state.selectedAccount?.merchantId ?: "" else ""
//                    val isCustomMerchantIdValid = customMerchantId.startsWith("acct_") && customMerchantId.length > 5
//                    TextField(
//                        value = customMerchantId,
//                        onValueChange = { newValue ->
//                            val newSelectedMerchant = state.accounts?.first { it.merchantId == newValue }
//                            viewModel.onAccountSelected(newSelectedMerchant!!)
//                        },
//                        label = { Text(stringResource(R.string.other)) },
//                        placeholder = { Text(stringResource(R.string.account_id_placeholder)) },
//                        isError = customMerchantId.isNotEmpty() && !isCustomMerchantIdValid,
//                        modifier = Modifier.weight(1f)
//                    )
//
//                }
//            }
//
//            item {
//                Spacer(modifier = Modifier.height(16.dp))
//                Text(stringResource(R.string.component_settings), style = MaterialTheme.typography.h6)
//                Spacer(modifier = Modifier.height(8.dp))
//                Button(
//                    onClick = { navController.navigate("onboarding_settings") }
//                ) {
//                    Text(stringResource(R.string.account_onboarding))
//                }
//                Button(
//                    onClick = { navController.navigate("presentation_settings") }
//                ) {
//                    Text(stringResource(R.string.view_controller_options))
//                }
//            }
//
//            item {
//                Spacer(modifier = Modifier.height(16.dp))
//                Text(stringResource(R.string.api_server_settings), style = MaterialTheme.typography.h6)
//                Spacer(modifier = Modifier.height(8.dp))
//                TextField(
//                    value = state.serverUrl,
//                    onValueChange = { newValue: String -> viewModel.onServerUrlChanged(newValue) },
//                    label = { Text(stringResource(R.string.server_url_label)) },
//                    placeholder = { Text(stringResource(R.string.server_url_placeholder)) },
//                    isError = state.serverUrl.isNotEmpty() && !isValidUrl(state.serverUrl),
//                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
//                    modifier = Modifier.fillMaxWidth(),
//                )
//                Button(
//                    onClick = { viewModel.onServerUrlChanged(SettingsService.DEFAULT_SERVER_BASE_URL) },
//                    enabled = SettingsService.DEFAULT_SERVER_BASE_URL != state.serverUrl
//                ) {
//                    Text(stringResource(R.string.reset_to_default))
//                }
//            }
//        }
//    }
}

private fun isValidUrl(url: String): Boolean {
    return try {
        URL(url).toURI()
        true
    } catch (e: Exception) {
        false
    }
}
