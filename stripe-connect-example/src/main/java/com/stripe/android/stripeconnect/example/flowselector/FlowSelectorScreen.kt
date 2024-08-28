package com.stripe.android.stripeconnect.example.flowselector

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.stripeconnect.Appearance
import com.stripe.android.stripeconnect.StripeConnect
import com.stripe.android.stripeconnect.StripeConnectComponent
import com.stripe.android.stripeconnect.example.appearances

@Composable
fun FlowSelectorScreen() {
    val viewModel: FlowSelectorViewModel = viewModel()

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val stripeConnect by remember(context) { mutableStateOf(StripeConnect.create(context)) }

    FlowSelectorContent(
        selectedAccount = uiState.selectedAccount,
        selectedFlow = uiState.selectedFlow,
        selectedAppearance = uiState.selectedAppearance,
        isLaunchButtonEnabled = uiState.isLaunchButtonEnabled,
        onSelectedFlowChanged = viewModel::onSelectedFlowChanged,
        onSelectedAccountChanged = viewModel::onSelectedAccountChanged,
        onSelectedAppearanceChanged = viewModel::onSelectedAppearanceChanged,
        onLaunchClicked = {
            val selectedComponent = uiState.selectedFlow
            val selectedAccount = uiState.selectedAccount
            val selectedAppearance = uiState.selectedAppearance
            if (selectedComponent != null && selectedAccount != null && selectedAppearance != null) {
                stripeConnect.launchComponent(
                    selectedComponent,
                    selectedAccount.accountId,
                    selectedAppearance.first,
                )
            }
        },
    )
}

@Composable
fun FlowSelectorContent(
    selectedFlow: StripeConnectComponent? = null,
    selectedAccount: StripeAccount? = null,
    selectedAppearance: Pair<Appearance?, String>? = null,
    isLaunchButtonEnabled: Boolean,
    onSelectedFlowChanged: (StripeConnectComponent?) -> Unit,
    onSelectedAccountChanged: (StripeAccount?) -> Unit,
    onSelectedAppearanceChanged: (Pair<Appearance?, String>?) -> Unit,
    onLaunchClicked: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AccountSelectorDropdown(selectedAccount = selectedAccount, onAccountSelected = onSelectedAccountChanged)

        Spacer(modifier = Modifier.padding(4.dp))
        FlowSelectorDropdown(selectedFlow = selectedFlow, onFlowSelected = onSelectedFlowChanged)
        Spacer(modifier = Modifier.padding(4.dp))
        AppearanceSelectorDropdown(selectedAppearance, onSelectedAppearanceChanged)
        Spacer(modifier = Modifier.padding(4.dp))

        Button(
            modifier = Modifier.padding(16.dp),
            enabled = isLaunchButtonEnabled,
            onClick = onLaunchClicked,
        ) {
            Text("Launch")
        }
    }
}

@Composable
fun FlowSelectorDropdown(
    selectedFlow: StripeConnectComponent? = null,
    onFlowSelected: (StripeConnectComponent) -> Unit,
) {
    var isDropdownExpanded: Boolean by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .clickable { isDropdownExpanded = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = selectedFlow?.getDisplayTitle() ?: "Select a flow")
            Icon(Icons.Filled.ExpandMore, contentDescription = "Expand flow dropdown")
        }
        DropdownMenu(expanded = isDropdownExpanded, onDismissRequest = { isDropdownExpanded = false }) {
            StripeConnectComponent.entries.forEachIndexed { index, flow ->
                DropdownMenuItem(
                    onClick = {
                        onFlowSelected(flow)
                        isDropdownExpanded = false
                    },
                ) {
                    Text(flow.getDisplayTitle())
                }
                if (index != StripeConnectComponent.entries.lastIndex) { Divider() }
            }
        }
    }
}

@Composable
fun AccountSelectorDropdown(
    selectedAccount: StripeAccount? = null,
    onAccountSelected: (StripeAccount) -> Unit,
) {
    var isDropdownExpanded: Boolean by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .clickable { isDropdownExpanded = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = selectedAccount?.displayTitle ?: "Select an Account")
            Icon(Icons.Filled.ExpandMore, contentDescription = "Expand account dropdown")
        }
        DropdownMenu(expanded = isDropdownExpanded, onDismissRequest = { isDropdownExpanded = false }) {
            StripeAccount.entries.forEachIndexed { index, account ->
                DropdownMenuItem(
                    onClick = {
                        onAccountSelected(account)
                        isDropdownExpanded = false
                    },
                ) {
                    Text(account.displayTitle)
                }
                if (index != StripeAccount.entries.lastIndex) { Divider() }
            }
        }
    }
}

@Composable
fun AppearanceSelectorDropdown(
    selectedAppearance: Pair<Appearance?, String>? = null,
    onSelectedAppearanceChanged: (Pair<Appearance?, String>?) -> Unit,
) {
    var isDropdownExpanded: Boolean by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .clickable { isDropdownExpanded = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = selectedAppearance?.second ?: "Select an Appearance")
            Icon(Icons.Filled.ExpandMore, contentDescription = "Expand appearance dropdown")
        }
        DropdownMenu(expanded = isDropdownExpanded, onDismissRequest = { isDropdownExpanded = false }) {
            appearances.forEachIndexed { index, (appearance, title) ->
                DropdownMenuItem(
                    onClick = {
                        onSelectedAppearanceChanged(appearance to title)
                        isDropdownExpanded = false
                    },
                ) {
                    Text(title)
                }
                if (index != appearances.lastIndex) { Divider() }
            }
        }
    }
}