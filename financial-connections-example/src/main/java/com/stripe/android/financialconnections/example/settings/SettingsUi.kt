package com.stripe.android.financialconnections.example.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Checkbox
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsUi(
    playgroundSettings: PlaygroundSettings,
    onSettingsChanged: (PlaygroundSettings) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.animateContentSize(),
    ) {
        for (setting in playgroundSettings.displayableSettings) {
            SingleSelectSetting(setting, playgroundSettings, onSettingsChanged)
        }
    }
}

@Composable
private fun <T> SingleSelectSetting(
    setting: Setting<T>,
    playgroundSettings: PlaygroundSettings,
    onSettingsChanged: (PlaygroundSettings) -> Unit
) {
    when (setting) {
        is SingleChoiceSetting -> SingleSelectSetting(
            name = setting.displayName,
            options = setting.options,
            value = setting.selectedOption
        ) {
            onSettingsChanged(
                playgroundSettings.withValue(setting, it)
            )
        }

        is MultipleChoiceSetting<*> -> MultiSelectSetting(
            name = setting.displayName,
            options = setting.options as List<Option<T>>,
            selectedValues = setting.selectedOption as List<T>
        ) {
            onSettingsChanged(
                playgroundSettings.withValue(setting as Setting<T>, it as T)
            )
        }
    }
}

@Composable
private fun <T> SingleSelectSetting(
    name: String,
    options: List<Option<T>>,
    value: T,
    onOptionChanged: (T) -> Unit,
) {
    when {
        value is Boolean -> {
            ToggleSetting(
                name = name,
                value = value,
                onOptionChanged = onOptionChanged as (Boolean) -> Unit,
            )
        }
        options.isEmpty() && value is String -> {
            @Suppress("UNCHECKED_CAST")
            TextSetting(
                name = name,
                value = value as String,
                onOptionChanged = onOptionChanged as (String) -> Unit,
            )
        }
        options.size < MAX_RADIO_BUTTON_OPTIONS -> {
            RadioButtonSetting(
                name = name,
                options = options,
                value = value,
                onOptionChanged = onOptionChanged,
            )
        }
        else -> {
            DropdownSetting(
                name = name,
                options = options,
                value = value,
                onOptionChanged = onOptionChanged,
            )
        }
    }
}

@Composable
fun ToggleSetting(
    name: String,
    value: Boolean,
    onOptionChanged: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
        )
        Switch(
            modifier = Modifier.padding(0.dp).defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
            checked = value,
            onCheckedChange = {
                onOptionChanged(it)
            },
        )
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalLayoutApi::class)
@Composable
private fun <T> MultiSelectSetting(
    name: String,
    options: List<Option<T>>,
    selectedValues: List<T>,
    onOptionChanged: (List<T>) -> Unit
) {
    Column {
        Row {
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
            )
        }

        FlowRow {
            options.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 5.dp)
                ) {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                        Checkbox(
                            checked = selectedValues.contains(option.value),
                            onCheckedChange = {
                                val newOptions = if (selectedValues.contains(option.value)) {
                                    selectedValues - option.value
                                } else {
                                    selectedValues + option.value
                                }
                                onOptionChanged(newOptions)
                            },
                        )
                    }
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
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done,
        ),
        onValueChange = { newValue: String ->
            onOptionChanged(newValue)
        },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTagsAsResourceId = true }
            .testTag("$name setting"),
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
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

        FlowRow {
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
