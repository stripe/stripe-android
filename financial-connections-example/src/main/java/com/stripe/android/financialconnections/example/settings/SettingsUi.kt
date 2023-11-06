package com.stripe.android.financialconnections.example.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Checkbox
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsUi(
    playgroundSettings: PlaygroundSettings,
    onSettingsChanged: (PlaygroundSettings) -> Unit,
) {
    Column {
        for (settingDefinition in playgroundSettings.settings) {
            Row(modifier = Modifier.padding(bottom = 16.dp)) {
                Setting(settingDefinition, playgroundSettings, onSettingsChanged)
            }
        }
    }
}

@Composable
private fun <T> Setting(
    settingDefinition: Setting<T>,
    playgroundSettings: PlaygroundSettings,
    onSettingsChanged: (PlaygroundSettings) -> Unit
) {
    when (settingDefinition) {
        is SingleChoiceSetting -> Setting(
            name = settingDefinition.displayName,
            options = settingDefinition.options,
            value = settingDefinition.selectedOption
        ) {
            onSettingsChanged(
                playgroundSettings.withValue(settingDefinition, it)
            )
        }

        is MultipleChoiceSetting<*> -> TODO()

    }
}

@Composable
private fun <T> Setting(
    name: String,
    options: List<Option<T>>,
    value: T,
    onOptionChanged: (T) -> Unit,
) {
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
private fun <T> MultiSelectSetting(
    name: String,
    options: List<Option<T>>,
    value: T,
    onOptionChanged: (T) -> Unit
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
            val selectedOptions = remember(value) {
                options.filter { option -> option.value == value }
            }
            options.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .selectable(
                            selected = selectedOptions.contains(option),
                            onClick = {
                                val newOptions = if (selectedOptions.contains(option)) {
                                    selectedOptions - option
                                } else {
                                    selectedOptions + option
                                }
                                onOptionChanged(
                                    newOptions
                                        .map { it.value }
                                        .first()
                                )
                            }
                        )
                        .padding(end = 5.dp)
                ) {
                    Checkbox(
                        checked = selectedOptions.contains(option),
                        onCheckedChange = null,
                    )
                    Text(
                        text = option.name,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TextSetting(
    name: String,
    value: String,
    onOptionChanged: (String) -> Unit,
) {
    OutlinedTextField(
        placeholder = { Text(text = name) },
        label = { Text(text = name) },
        value = value,
        onValueChange = { newValue: String ->
            onOptionChanged(newValue)
        },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTagsAsResourceId = true }
            .testTag("$name setting"),
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun <T> RadioButtonSetting(
    name: String,
    options: List<Option<T>>,
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
                        modifier = Modifier
                            .semantics { testTagsAsResourceId = true }
                            .testTag("${option.name} option"),
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

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun <T> DropdownSetting(
    name: String,
    options: List<Option<T>>,
    value: T,
    onOptionChanged: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = remember(value) { options.firstOrNull { it.value == value } }

    ExposedDropdownMenuBox(
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag("$name setting"),
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
                    modifier = Modifier
                        .semantics { testTagsAsResourceId = true }
                        .testTag("${option.name} option"),
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
