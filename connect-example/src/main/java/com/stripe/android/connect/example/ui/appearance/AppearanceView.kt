package com.stripe.android.connect.example.ui.appearance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.connect.appearance.TextTransform
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.ui.common.ConnectExampleScaffold
import com.stripe.android.connect.example.ui.settings.SettingsDropdownField
import com.stripe.android.connect.example.ui.settings.SettingsNavigationItem
import com.stripe.android.connect.example.ui.settings.SettingsSectionHeader
import com.stripe.android.uicore.R as StripeUiCoreR

@Composable
fun AppearanceView(
    viewModel: AppearanceViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showTokenEditor by remember { mutableStateOf(false) }

    if (showTokenEditor) {
        CustomThemeView(
            customThemeOverrides = state.customThemeOverrides,
            onOverridesChanged = viewModel::onOverridesChanged,
            onClearOverrides = viewModel::clearOverrides,
            onBack = { showTokenEditor = false },
            onSave = {
                viewModel.saveAppearance()
                showTokenEditor = false
            },
        )
    } else {
        AppearancePickerView(
            state = state,
            onDismiss = onDismiss,
            onAppearanceSelected = viewModel::onAppearanceSelected,
            onSave = {
                viewModel.saveAppearance()
                onDismiss()
            },
            onEditCustomTheme = { showTokenEditor = true },
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun AppearancePickerView(
    state: AppearanceViewModel.AppearanceState,
    onDismiss: () -> Unit,
    onAppearanceSelected: (AppearanceInfo.AppearanceId) -> Unit,
    onSave: () -> Unit,
    onEditCustomTheme: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    ConnectExampleScaffold(
        title = stringResource(R.string.customize_appearance),
        navigationIcon = {
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(StripeUiCoreR.drawable.stripe_ic_material_close),
                    contentDescription = stringResource(R.string.cancel)
                )
            }
        },
        actions = {
            IconButton(
                enabled = state.saveEnabled,
                onClick = onSave,
            ) {
                Icon(
                    painter = painterResource(StripeUiCoreR.drawable.stripe_ic_checkmark),
                    contentDescription = stringResource(R.string.save)
                )
            }
        }
    ) {
        Column {
            SettingsSectionHeader(
                text = "Select Theme",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            state.appearances.forEach { appearance ->
                Row(
                    modifier = Modifier
                        .clickable { onAppearanceSelected(appearance) }
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = appearance == state.selectedAppearance,
                        onClick = null,
                    )
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = stringResource(appearance.displayNameRes)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionHeader(
                text = "Custom Theme",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            SettingsNavigationItem(
                text = "Edit custom theme values",
                onClick = onEditCustomTheme,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun CustomThemeView(
    customThemeOverrides: CustomThemeOverridesState,
    onOverridesChanged: (CustomThemeOverridesState) -> Unit,
    onClearOverrides: () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    BackHandler(onBack = onBack)
    ConnectExampleScaffold(
        title = "Custom Theme",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(StripeUiCoreR.drawable.stripe_ic_material_arrow_back),
                    contentDescription = stringResource(R.string.cancel)
                )
            }
        },
        actions = {
            IconButton(onClick = onSave) {
                Icon(
                    painter = painterResource(StripeUiCoreR.drawable.stripe_ic_checkmark),
                    contentDescription = stringResource(R.string.save)
                )
            }
        }
    ) {
        LazyColumn {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Choose your own values for the appearance",
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onClearOverrides) {
                        Text("Clear All")
                    }
                }
            }
            item {
                TokenSectionHeader("Button")
                TextTransformDropdown(
                    label = "buttonLabelTextTransform",
                    value = customThemeOverrides.buttonLabelTextTransform,
                    onValueChange = { onOverridesChanged(customThemeOverrides.copy(buttonLabelTextTransform = it)) },
                )
                TokenTextField(
                    label = "buttonLabelFontWeight",
                    placeholder = "e.g. 600",
                    value = customThemeOverrides.buttonLabelFontWeight,
                    onValueChange = { onOverridesChanged(customThemeOverrides.copy(buttonLabelFontWeight = it)) },
                )
                TokenTextField(
                    label = "buttonLabelFontSize",
                    placeholder = "e.g. 15",
                    value = customThemeOverrides.buttonLabelFontSize,
                    onValueChange = { onOverridesChanged(customThemeOverrides.copy(buttonLabelFontSize = it)) },
                )
                TokenTextField(
                    label = "buttonPaddingY",
                    placeholder = "e.g. 12",
                    value = customThemeOverrides.buttonPaddingY,
                    onValueChange = { onOverridesChanged(customThemeOverrides.copy(buttonPaddingY = it)) },
                )
                TokenTextField(
                    label = "buttonPaddingX",
                    placeholder = "e.g. 20",
                    value = customThemeOverrides.buttonPaddingX,
                    onValueChange = { onOverridesChanged(customThemeOverrides.copy(buttonPaddingX = it)) },
                )
                TokenTextField(
                    label = "buttonDangerColorBackground",
                    placeholder = "hex e.g. FF0000",
                    value = customThemeOverrides.buttonDangerColorBackground,
                    onValueChange = { onOverridesChanged(customThemeOverrides.copy(buttonDangerColorBackground = it)) },
                )
                TokenTextField(
                    label = "buttonDangerColorBorder",
                    placeholder = "hex e.g. CC0000",
                    value = customThemeOverrides.buttonDangerColorBorder,
                    onValueChange = { onOverridesChanged(customThemeOverrides.copy(buttonDangerColorBorder = it)) },
                )
                TokenTextField(
                    label = "buttonDangerColorText",
                    placeholder = "hex e.g. FFFFFF",
                    value = customThemeOverrides.buttonDangerColorText,
                    onValueChange = { onOverridesChanged(customThemeOverrides.copy(buttonDangerColorText = it)) },
                )
            }
            item {
                TokenSectionHeader("Badge")
                TextTransformDropdown(
                    label = "badgeLabelTextTransform",
                    value = customThemeOverrides.badgeLabelTextTransform,
                    onValueChange = { onOverridesChanged(customThemeOverrides.copy(badgeLabelTextTransform = it)) },
                )
                TokenTextField(
                    label = "badgeLabelFontWeight",
                    placeholder = "e.g. 600",
                    value = customThemeOverrides.badgeLabelFontWeight,
                    onValueChange = { onOverridesChanged(customThemeOverrides.copy(badgeLabelFontWeight = it)) },
                )
                TokenTextField(
                    label = "badgeLabelFontSize",
                    placeholder = "e.g. 12",
                    value = customThemeOverrides.badgeLabelFontSize,
                    onValueChange = { onOverridesChanged(customThemeOverrides.copy(badgeLabelFontSize = it)) },
                )
                TokenTextField(
                    label = "badgePaddingY",
                    placeholder = "e.g. 4",
                    value = customThemeOverrides.badgePaddingY,
                    onValueChange = { onOverridesChanged(customThemeOverrides.copy(badgePaddingY = it)) },
                )
                TokenTextField(
                    label = "badgePaddingX",
                    placeholder = "e.g. 10",
                    value = customThemeOverrides.badgePaddingX,
                    onValueChange = { onOverridesChanged(customThemeOverrides.copy(badgePaddingX = it)) },
                )
            }
            item {
                TokenSectionHeader("Action")
                TextTransformDropdown(
                    label = "actionPrimaryTextTransform",
                    value = customThemeOverrides.actionPrimaryTextTransform,
                    onValueChange = { onOverridesChanged(customThemeOverrides.copy(actionPrimaryTextTransform = it)) },
                )
                TextTransformDropdown(
                    label = "actionSecondaryTextTransform",
                    value = customThemeOverrides.actionSecondaryTextTransform,
                    onValueChange = {
                        onOverridesChanged(customThemeOverrides.copy(actionSecondaryTextTransform = it))
                    },
                )
            }
            item {
                TokenSectionHeader("Form")
                TokenTextField(
                    label = "formPlaceholderTextColor",
                    placeholder = "hex e.g. 999999",
                    value = customThemeOverrides.formPlaceholderTextColor,
                    onValueChange = { onOverridesChanged(customThemeOverrides.copy(formPlaceholderTextColor = it)) },
                )
                TokenTextField(
                    label = "inputFieldPaddingX",
                    placeholder = "e.g. 14",
                    value = customThemeOverrides.inputFieldPaddingX,
                    onValueChange = { onOverridesChanged(customThemeOverrides.copy(inputFieldPaddingX = it)) },
                )
                TokenTextField(
                    label = "inputFieldPaddingY",
                    placeholder = "e.g. 10",
                    value = customThemeOverrides.inputFieldPaddingY,
                    onValueChange = { onOverridesChanged(customThemeOverrides.copy(inputFieldPaddingY = it)) },
                )
            }
            item {
                TokenSectionHeader("Table")
                TokenTextField(
                    label = "tableRowPaddingY",
                    placeholder = "e.g. 16",
                    value = customThemeOverrides.tableRowPaddingY,
                    onValueChange = { onOverridesChanged(customThemeOverrides.copy(tableRowPaddingY = it)) },
                )
            }
        }
    }
}

@Composable
private fun TokenSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.subtitle1,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun TokenTextField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        label = { Text(label, style = MaterialTheme.typography.caption) },
        placeholder = { Text(placeholder) },
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
    )
}

@Composable
private fun TextTransformDropdown(
    label: String,
    value: TextTransform,
    onValueChange: (TextTransform) -> Unit,
) {
    SettingsDropdownField(
        label = label,
        options = TextTransform.entries,
        selectedOption = value,
        onSelectOption = onValueChange,
        optionToString = { it.name },
    )
}
