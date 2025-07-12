package com.stripe.android.paymentsheet.example.playground.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.example.playground.PlaygroundTheme
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun SettingsUi(
    playgroundSettings: PlaygroundSettings,
    searchQuery: String,
) {
    val configurationData by playgroundSettings.configurationData.collectAsState()
    val displayableDefinitions by playgroundSettings.displayableDefinitions.collectAsState()
    val filteredDefinitions = remember(displayableDefinitions, searchQuery) {
        displayableDefinitions.filter { it.displayName.matchesQuery(searchQuery) }
    }

    Column(
        modifier = Modifier.padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (IntegrationTypeSettingName.matchesQuery(searchQuery)) {
            Row {
                IntegrationTypeConfigurableSetting(
                    configurationData,
                    playgroundSettings::updateConfigurationData
                )
            }
        }

        for (settingDefinition in filteredDefinitions) {
            Setting(settingDefinition, playgroundSettings)
        }
    }
}

private val WordBoundaryRegex by lazy(LazyThreadSafetyMode.NONE) { "\\s+".toRegex() }

/**
 * Returns true if the string matches the query.
 */
private fun String.matchesQuery(query: String): Boolean {
    if (query.isBlank()) {
        return true
    }

    val words = this.trim().split(WordBoundaryRegex)
    val queryWords = query.trim().split(WordBoundaryRegex)
    return queryWords.all { queryWord ->
        words.any { word -> word.startsWith(queryWord, ignoreCase = true) }
    }
}

@Preview
@Composable
private fun SettingsUiPreview() {
    PlaygroundTheme(
        content = {
            SettingsUi(
                playgroundSettings = PlaygroundSettings.createFromDefaults(),
                searchQuery = "",
            )
        },
        bottomBarContent = {},
        topBarContent = {}
    )
}

@Composable
private fun <T> Setting(
    settingDefinition: PlaygroundSettingDefinition.Displayable<T>,
    playgroundSettings: PlaygroundSettings,
) {
    val configurationData by playgroundSettings.configurationData.collectAsState()

    val options = remember(settingDefinition, configurationData) {
        settingDefinition.createOptions(configurationData)
    }

    Setting(
        name = settingDefinition.displayName,
        options = options,
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

private const val IntegrationTypeSettingName = "Integration Type"

@Composable
private fun IntegrationTypeConfigurableSetting(
    configurationData: PlaygroundConfigurationData,
    updateConfigurationData: (updater: (PlaygroundConfigurationData) -> PlaygroundConfigurationData) -> Unit
) {
    DropdownSetting(
        name = IntegrationTypeSettingName,
        options = listOf(
            PlaygroundSettingDefinition.Displayable.Option(
                name = "Payment Sheet",
                value = PlaygroundConfigurationData.IntegrationType.PaymentSheet
            ),
            PlaygroundSettingDefinition.Displayable.Option(
                name = "Flow Controller",
                value = PlaygroundConfigurationData.IntegrationType.FlowController
            ),
            PlaygroundSettingDefinition.Displayable.Option(
                name = "Flow Controller w/ SPT",
                value = PlaygroundConfigurationData.IntegrationType.FlowControllerWithSpt
            ),
            PlaygroundSettingDefinition.Displayable.Option(
                name = "Embedded",
                value = PlaygroundConfigurationData.IntegrationType.Embedded
            ),
            PlaygroundSettingDefinition.Displayable.Option(
                name = "Customer Sheet",
                value = PlaygroundConfigurationData.IntegrationType.CustomerSheet
            ),
            PlaygroundSettingDefinition.Displayable.Option(
                name = "Link Controller",
                value = PlaygroundConfigurationData.IntegrationType.LinkController
            ),
        ),
        value = configurationData.integrationType
    ) { integrationType ->
        updateConfigurationData { configurationData ->
            configurationData.copy(integrationType = integrationType)
        }
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

        val selectedOption = remember(options, value) {
            options.firstOrNull { it.value == value }
        }

        Row {
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

        Row {
            if (selectedOption == null) {
                Text(
                    text = value.toString(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun <T> DropdownSetting(
    name: String,
    options: List<PlaygroundSettingDefinition.Displayable.Option<T>>,
    value: T,
    onOptionChanged: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = remember(options, value) {
        options.firstOrNull { it.value == value }
    }

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
