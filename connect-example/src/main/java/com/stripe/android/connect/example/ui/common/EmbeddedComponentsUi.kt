package com.stripe.android.connect.example.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.connect.example.data.Merchant

@Composable
fun EmbeddedComponentsLauncherScreen(
    embeddedComponentName: String,
    selectedAccount: Merchant?,
    connectSDKAccounts: List<Merchant>,
    onConnectSDKAccountSelected: (Merchant) -> Unit,
    onEmbeddedComponentLaunched: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AccountSelector(
                selectedAccount = selectedAccount,
                accounts = connectSDKAccounts,
                onAccountSelected = onConnectSDKAccountSelected,
            )
            Button(
                onClick = onEmbeddedComponentLaunched,
                enabled = selectedAccount != null,
            ) {
                Text("Launch $embeddedComponentName")
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AccountSelector(
    selectedAccount: Merchant?,
    accounts: List<Merchant>,
    onAccountSelected: (Merchant) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        TextField(
            readOnly = true,
            value = selectedAccount?.displayName ?: "No account selected",
            onValueChange = {},
            label = { Text("Account") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            for (account in accounts) {
                DropdownMenuItem(
                    onClick = {
                        onAccountSelected(account)
                        expanded = false
                    }
                ) {
                    Text(text = account.displayName)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LaunchEmbeddedComponentsScreenPreviewWithSelectedAccount() {
    EmbeddedComponentsLauncherScreen(
        embeddedComponentName = "Payouts",
        selectedAccount = Merchant(merchantId = "1", displayName = "Selected Merchant"),
        connectSDKAccounts = listOf(
            Merchant(merchantId = "2", displayName = "Merchant 1"),
            Merchant(merchantId = "3", displayName = "Merchant 2"),
            Merchant(merchantId = "4", displayName = "Merchant 3")
        ),
        onConnectSDKAccountSelected = {},
        onEmbeddedComponentLaunched = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun LaunchEmbeddedComponentsScreenPreviewWithNoSelectedAccount() {
    EmbeddedComponentsLauncherScreen(
        embeddedComponentName = "Payouts",
        selectedAccount = null,
        connectSDKAccounts = listOf(
            Merchant(merchantId = "2", displayName = "Merchant 1"),
            Merchant(merchantId = "3", displayName = "Merchant 2"),
            Merchant(merchantId = "4", displayName = "Merchant 3")
        ),
        onConnectSDKAccountSelected = {},
        onEmbeddedComponentLaunched = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun LaunchEmbeddedComponentsScreenPreviewWithEmptyAccounts() {
    EmbeddedComponentsLauncherScreen(
        embeddedComponentName = "Payouts",
        selectedAccount = Merchant(merchantId = "1", displayName = "Selected Merchant"),
        connectSDKAccounts = emptyList(),
        onConnectSDKAccountSelected = {},
        onEmbeddedComponentLaunched = {}
    )
}
