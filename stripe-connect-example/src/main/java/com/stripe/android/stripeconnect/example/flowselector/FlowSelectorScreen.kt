package com.stripe.android.stripeconnect.example.flowselector

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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
import com.stripe.android.stripeconnect.StripeConnect
import com.stripe.android.stripeconnect.StripeConnectComponent

@Composable
fun FlowSelectorScreen() {
    val viewModel: FlowSelectorViewModel = viewModel()

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val stripeConnect by remember(context) { mutableStateOf(StripeConnect.create(context)) }

    FlowSelectorContent(
        selectedFlow = uiState.selectedFlow,
        isLaunchButtonEnabled = uiState.isLaunchButtonEnabled,
        onSelectedFlowChanged = viewModel::onSelectedFlowChanged,
        onLaunchClicked = {
            val selectedComponent = uiState.selectedFlow
            if (selectedComponent != null) {
                stripeConnect.launchComponent(selectedComponent)
            }
        },
    )
}

@Composable
fun FlowSelectorContent(
    selectedFlow: StripeConnectComponent? = null,
    isLaunchButtonEnabled: Boolean,
    onSelectedFlowChanged: (StripeConnectComponent?) -> Unit,
    onLaunchClicked: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FlowSelectorDropdown(selectedFlow = selectedFlow, onFlowSelected = onSelectedFlowChanged)
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