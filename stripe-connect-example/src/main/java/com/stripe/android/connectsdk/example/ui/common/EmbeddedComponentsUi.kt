package com.stripe.android.connectsdk.example.ui.common

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.connectsdk.EmbeddedComponentManager
import com.stripe.android.connectsdk.PrivateBetaConnectSDK
import com.stripe.android.connectsdk.example.networking.Merchant

@OptIn(PrivateBetaConnectSDK::class)
@Composable
fun LaunchEmbeddedComponentsScreen(
    embeddedComponentName: String,
    selectedAccount: Merchant?,
    selectedAppearance: Pair<EmbeddedComponentManager.AppearanceVariables, String>?,
    connectSDKAccounts: List<Merchant>,
    appearances: List<Pair<EmbeddedComponentManager.AppearanceVariables, String>>,
    onConnectSDKAccountSelected: (Merchant) -> Unit,
    onAppearanceChanged: (Pair<EmbeddedComponentManager.AppearanceVariables, String>?) -> Unit,
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
            AppearanceSelector(
                selectedAppearance = selectedAppearance,
                appearances = appearances,
                onAppearanceSelected = onAppearanceChanged,
            )
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
    selectedAccount: Merchant? = null,
    accounts: List<Merchant>,
    onAccountSelected: (Merchant) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
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
                    Text(text = account.displayName,)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, PrivateBetaConnectSDK::class)
@Composable
fun AppearanceSelector(
    selectedAppearance: Pair<EmbeddedComponentManager.AppearanceVariables, String>? = null,
    appearances: List<Pair<EmbeddedComponentManager.AppearanceVariables, String>>,
    onAppearanceSelected: (Pair<EmbeddedComponentManager.AppearanceVariables, String>?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        TextField(
            readOnly = true,
            value = selectedAppearance?.second ?: "Default", // display name
            onValueChange = {},
            label = { Text("Appearance") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                onClick = {
                    onAppearanceSelected(null)
                    expanded = false
                }
            ) {
                Text(text = "Default")
            }
            for (appearance in appearances) {
                DropdownMenuItem(
                    onClick = {
                        onAppearanceSelected(appearance)
                        expanded = false
                    }
                ) {
                    Text(text = appearance.second) // display name
                }
            }
        }
    }
}

@OptIn(PrivateBetaConnectSDK::class)
@Preview(showBackground = true)
@Composable
fun LaunchEmbeddedComponentsScreenPreviewWithSelectedAccount() {
    LaunchEmbeddedComponentsScreen(
        embeddedComponentName = "Payouts",
        selectedAppearance = null,
        selectedAccount = Merchant(merchantId = "1", displayName = "Selected Merchant"),
        connectSDKAccounts = listOf(
            Merchant(merchantId = "2", displayName = "Merchant 1"),
            Merchant(merchantId = "3", displayName = "Merchant 2"),
            Merchant(merchantId = "4", displayName = "Merchant 3")
        ),
        appearances = listOf(
            purpleHazeAppearance to "Purple Haze",
            ogreAppearance to "Ogre",
            protanopiaAppearance to "Protanopia",
            oceanBreezeAppearance to "Ocean Breeze",
            hotDogAppearance to "Hot Dog",
        ),
        onConnectSDKAccountSelected = {},
        onAppearanceChanged = {},
        onEmbeddedComponentLaunched = {}
    )
}

@OptIn(PrivateBetaConnectSDK::class)
@Preview(showBackground = true)
@Composable
fun LaunchEmbeddedComponentsScreenPreviewWithNoSelectedAccount() {
    LaunchEmbeddedComponentsScreen(
        embeddedComponentName = "Payouts",
        selectedAccount = null,
        selectedAppearance = null,
        connectSDKAccounts = listOf(
            Merchant(merchantId = "2", displayName = "Merchant 1"),
            Merchant(merchantId = "3", displayName = "Merchant 2"),
            Merchant(merchantId = "4", displayName = "Merchant 3")
        ),
        appearances = listOf(
            purpleHazeAppearance to "Purple Haze",
            ogreAppearance to "Ogre",
            protanopiaAppearance to "Protanopia",
            oceanBreezeAppearance to "Ocean Breeze",
            hotDogAppearance to "Hot Dog",
        ),
        onConnectSDKAccountSelected = {},
        onAppearanceChanged = {},
        onEmbeddedComponentLaunched = {}
    )
}

@OptIn(PrivateBetaConnectSDK::class)
@Preview(showBackground = true)
@Composable
fun LaunchEmbeddedComponentsScreenPreviewWithEmptyAccounts() {
    LaunchEmbeddedComponentsScreen(
        embeddedComponentName = "Payouts",
        selectedAppearance = null,
        selectedAccount = Merchant(merchantId = "1", displayName = "Selected Merchant"),
        connectSDKAccounts = emptyList(),
        appearances = listOf(
            purpleHazeAppearance to "Purple Haze",
            ogreAppearance to "Ogre",
            protanopiaAppearance to "Protanopia",
            oceanBreezeAppearance to "Ocean Breeze",
            hotDogAppearance to "Hot Dog",
        ),
        onConnectSDKAccountSelected = {},
        onAppearanceChanged = {},
        onEmbeddedComponentLaunched = {}
    )
}
