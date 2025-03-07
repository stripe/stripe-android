package com.stripe.android.connect.example.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.data.PresentationSettings
import com.stripe.android.connect.example.ui.common.BackIconButton
import com.stripe.android.connect.example.ui.common.ConnectExampleScaffold

@Composable
fun PresentationSettingsView(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val presentationSettings = state.presentationSettings
    PresentationSettingsView(
        presentationSettings = presentationSettings,
        onBack = onBack,
        onSave = { viewModel.onPresentationSettingsConfirmed(it) },
    )
}

@Composable
private fun PresentationSettingsView(
    presentationSettings: PresentationSettings,
    onBack: () -> Unit,
    onSave: (PresentationSettings) -> Unit,
) {
    var enableEdgeToEdge by rememberSaveable { mutableStateOf(presentationSettings.enableEdgeToEdge) }
    ConnectExampleScaffold(
        title = stringResource(R.string.presentation_settings),
        navigationIcon = { BackIconButton(onBack) },
        actions = {
            IconButton(
                onClick = {
                    onSave(
                        PresentationSettings(
                            presentationStyleIsPush = presentationSettings.presentationStyleIsPush,
                            embedInTabBar = presentationSettings.embedInTabBar,
                            embedInNavBar = presentationSettings.embedInNavBar,
                            enableEdgeToEdge = enableEdgeToEdge,
                        )
                    )
                    onBack()
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.save)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSwitchItem(
                text = stringResource(R.string.enable_edge_to_edge),
                isChecked = enableEdgeToEdge,
                onCheckedChange = { enableEdgeToEdge = it }
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    text: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = text,
        )
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Preview
@Composable
private fun PresentationSettingsViewPreview() {
    PresentationSettingsView(
        presentationSettings = PresentationSettings(),
        onBack = {},
        onSave = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsSwitchItemPreview() {
    SettingsSwitchItem(
        text = "Example",
        isChecked = true,
        onCheckedChange = { },
    )
}
