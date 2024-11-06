package com.stripe.android.connect.example.ui.appearance

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
fun AppearanceView(
    onDismiss: () -> Unit,
    viewModel: AppearanceViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    BackHandler { onDismiss() }
    MainContent(
        title = stringResource(R.string.customize_appearance),
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
                    viewModel.saveAppearance()
                    onDismiss()
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.save)
                )
            }
        }
    ) {
        SelectAnAppearance(
            appearances = state.appearances,
            selectedAppearance = state.selectedAppearance,
            onAppearanceSelected = viewModel::onAppearanceSelected,
        )
    }
}

@Composable
private fun SelectAnAppearance(
    appearances: List<AppearanceInfo.AppearanceId>,
    selectedAppearance: AppearanceInfo.AppearanceId?,
    onAppearanceSelected: (AppearanceInfo.AppearanceId) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        appearances.forEach { appearance ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAppearanceSelected(appearance) },
            ) {
                RadioButton(
                    selected = appearance == selectedAppearance,
                    onClick = null, // onClick handled by row
                )
                Column {
                    Text(text = stringResource(appearance.displayNameRes))
                }
            }
        }
    }
}