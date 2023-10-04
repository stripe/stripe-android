package com.stripe.android.paymentsheet.example.playground.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun SettingsUi(playgroundSettings: PlaygroundSettings) {
    Column {
        for (settingDefinition in PlaygroundSettings.uiSettingDefinitions) {
            Row(modifier = Modifier.padding(bottom = 16.dp)) {
                Setting(settingDefinition, playgroundSettings)
            }
        }
    }
}

@Composable
private fun <T> Setting(
    settingDefinition: PlaygroundSettingDefinition.Displayable<T>,
    playgroundSettings: PlaygroundSettings,
) {
    Setting(
        name = settingDefinition.displayName,
        options = settingDefinition.options,
        valueFlow = playgroundSettings[settingDefinition],
    ) { newValue ->
        playgroundSettings[settingDefinition] = newValue
    }
}

@Composable
private fun <T> Setting(
    name: String,
    options: List<PlaygroundSettingDefinition.Displayable.Option<T>>,
    valueFlow: StateFlow<T>,
    onOptionChanged: (T) -> Unit,
) {
    val value by valueFlow.collectAsState()
    if (options.isEmpty() && value is String) {
        @Suppress("UNCHECKED_CAST")
        TextSetting(
            name = name,
            value = value as String,
            onOptionChanged = onOptionChanged as (String) -> Unit,
        )
    } else if (options.size < MAX_RADIO_BUTTON_OPTIONS) {
        RadioButtonSetting(
            name = name,
            options = options,
            value = value,
            onOptionChanged = onOptionChanged,
        )
    } else {
        DropdownSetting(
            name = name,
            options = options,
            value = value,
            onOptionChanged = onOptionChanged,
        )
    }
}

@Composable
private fun TextSetting(
    name: String,
    value: String,
    onOptionChanged: (String) -> Unit,
) {
    TextField(
        placeholder = { Text(text = name) },
        label = { Text(text = name) },
        value = value,
        onValueChange = { newValue: String ->
            onOptionChanged(newValue)
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun <T> RadioButtonSetting(
    name: String,
    options: List<PlaygroundSettingDefinition.Displayable.Option<T>>,
    value: T,
    onOptionChanged: (T) -> Unit,
) {
    Column {
        Row {
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 5.dp)
            )
        }

        Row {
            val selectedOption = remember(value) { options.firstOrNull { it.value == value } }
            options.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .selectable(
                            selected = (option == selectedOption),
                            onClick = {
                                onOptionChanged(option.value)
                            }
                        )
                        .padding(end = 5.dp)
                ) {
                    RadioButton(
                        selected = (option == selectedOption),
                        onClick = null,
                    )
                    Text(
                        text = option.name,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun <T> DropdownSetting(
    name: String,
    options: List<PlaygroundSettingDefinition.Displayable.Option<T>>,
    value: T,
    onOptionChanged: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = remember(value) { options.firstOrNull { it.value == value } }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        TextField(
            readOnly = true,
            value = selectedOption?.name.orEmpty(),
            onValueChange = {},
            label = { Text(name) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            for (option in options) {
                DropdownMenuItem(
                    onClick = {
                        onOptionChanged(option.value)
                        expanded = false
                    }
                ) {
                    Text(
                        text = option.name,
                    )
                }
            }
        }
    }
}

private const val MAX_RADIO_BUTTON_OPTIONS = 4
