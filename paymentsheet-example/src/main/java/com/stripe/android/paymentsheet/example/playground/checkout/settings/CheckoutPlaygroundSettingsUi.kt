package com.stripe.android.paymentsheet.example.playground.checkout.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import com.stripe.android.paymentsheet.example.playground.matchesQuery

internal const val CheckoutSettingsScreenTestTag = "checkout_settings_screen"
private const val CheckoutSettingGroupTestTagPrefix = "checkout_setting_group:"
private const val CheckoutSettingValueTestTagPrefix = "checkout_setting_value:"
private const val CheckoutSettingBreadcrumbTestTagPrefix = "checkout_setting_breadcrumb:"

@Composable
internal fun CheckoutPlaygroundSettingsUi(
    configuration: CheckoutPlaygroundSettingDefinition.Configuration,
    searchQuery: String,
    settings: CheckoutPlaygroundSettings,
    onOpenConfiguration: (CheckoutPlaygroundSettingDefinition.Configuration) -> Unit,
) {
    val values by settings.values.collectAsState()
    val searchResults = remember(searchQuery) {
        CheckoutPlaygroundDefinitions.root.searchResults(
            query = searchQuery,
            parents = emptyList(),
        )
    }
    Column(
        modifier = Modifier.testTag(CheckoutSettingsScreenTestTag),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (searchQuery.isBlank()) {
            ConfigurationContent(
                configuration = configuration,
                values = values,
                settings = settings,
                onOpenConfiguration = onOpenConfiguration,
            )
        } else if (searchResults.isEmpty()) {
            Text("No matching settings found")
        } else {
            SearchResultsContent(
                results = searchResults,
                values = values,
                settings = settings,
            )
        }
    }
}

@Composable
private fun ConfigurationContent(
    configuration: CheckoutPlaygroundSettingDefinition.Configuration,
    values: Map<CheckoutPlaygroundSettingDefinition.Value<*>, String>,
    settings: CheckoutPlaygroundSettings,
    onOpenConfiguration: (CheckoutPlaygroundSettingDefinition.Configuration) -> Unit,
) {
    configuration.children.filter { definition ->
        definition !is CheckoutPlaygroundSettingDefinition.Value<*> || definition.isApplicable(settings)
    }.forEach { definition ->
        when (definition) {
            is CheckoutPlaygroundSettingDefinition.Configuration -> ConfigurationRow(
                configuration = definition,
                onClick = { onOpenConfiguration(definition) },
            )
            is CheckoutPlaygroundSettingDefinition.Value<*> -> ValueRow(
                definition = definition,
                value = requireNotNull(values[definition]),
                enabled = true,
                onValueChanged = { settings.updateSerialized(definition, it) },
            )
        }
    }
}

@Composable
private fun SearchResultsContent(
    results: List<SearchResult>,
    values: Map<CheckoutPlaygroundSettingDefinition.Value<*>, String>,
    settings: CheckoutPlaygroundSettings,
) {
    results.forEach { result ->
        val enabled = result.definition.isApplicable(settings)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = result.breadcrumb,
                modifier = Modifier.testTag(checkoutSettingBreadcrumbTestTag(result.definition)),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
            )
            ValueRow(
                definition = result.definition,
                value = requireNotNull(values[result.definition]),
                enabled = enabled,
                onValueChanged = { value ->
                    if (enabled) {
                        settings.updateSerialized(result.definition, value)
                    }
                },
            )
        }
    }
}

private data class SearchResult(
    val definition: CheckoutPlaygroundSettingDefinition.Value<*>,
    val breadcrumb: String,
)

private fun CheckoutPlaygroundSettingDefinition.Configuration.searchResults(
    query: String,
    parents: List<String>,
): List<SearchResult> {
    return children.flatMap { definition ->
        when (definition) {
            is CheckoutPlaygroundSettingDefinition.Configuration -> {
                definition.searchResults(
                    query = query,
                    parents = parents + definition.displayName,
                )
            }
            is CheckoutPlaygroundSettingDefinition.Value<*> -> {
                if (definition.displayName.matchesQuery(query)) {
                    listOf(
                        SearchResult(
                            definition = definition,
                            breadcrumb = parents.joinToString(" › "),
                        )
                    )
                } else {
                    emptyList()
                }
            }
        }
    }
}

@Composable
private fun ConfigurationRow(
    configuration: CheckoutPlaygroundSettingDefinition.Configuration,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(checkoutSettingGroupTestTag(configuration))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = configuration.displayName,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
        )
        Text(text = "›", style = MaterialTheme.typography.h5)
    }
}

@Composable
private fun <T> ValueRow(
    definition: CheckoutPlaygroundSettingDefinition.Value<T>,
    value: String,
    enabled: Boolean,
    onValueChanged: (String) -> Unit,
) {
    if (definition.input == CheckoutPlaygroundSettingDefinition.Value.Input.Color) {
        ColorValue(definition, value, enabled, onValueChanged)
    } else if (definition.options.isEmpty()) {
        TextValue(definition, value, enabled, onValueChanged)
    } else if (definition.options.size <= MaxInlineOptions) {
        RadioValue(definition, value, enabled, onValueChanged)
    } else {
        DropdownValue(definition, value, enabled, onValueChanged)
    }
}

@Composable
private fun <T> ColorValue(
    definition: CheckoutPlaygroundSettingDefinition.Value<T>,
    value: String,
    enabled: Boolean,
    onValueChanged: (String) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val selectedColor = value.toComposeColorOrNull()
    LaunchedEffect(enabled) {
        if (!enabled) {
            showPicker = false
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(checkoutSettingValueTestTag(definition))
            .alpha(if (enabled) 1f else ContentAlpha.disabled)
            .clickable(enabled = enabled) { showPicker = true }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(text = definition.displayName, fontWeight = FontWeight.Bold)
            Text(text = value.ifEmpty { "Default" }, style = MaterialTheme.typography.caption)
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(selectedColor ?: Color.Transparent)
                .border(1.dp, MaterialTheme.colors.onSurface, CircleShape),
        )
    }

    if (showPicker) {
        ColorPickerDialog(
            initialColor = selectedColor ?: Color.Black,
            onDismiss = { showPicker = false },
            onClear = {
                onValueChanged("")
                showPicker = false
            },
            onColorPicked = {
                onValueChanged(it.toSerializedColor())
                showPicker = false
            },
        )
    }
}

@Composable
private fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onColorPicked: (Color) -> Unit,
) {
    var currentColor by remember(initialColor) { mutableStateOf(initialColor) }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            ClassicColorPicker(
                modifier = Modifier.fillMaxSize(),
                color = HsvColor.from(currentColor),
                onColorChanged = { currentColor = it.toColor() },
            )
        },
        buttons = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onClear,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Use default")
                }
                Button(
                    onClick = { onColorPicked(currentColor) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = currentColor),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Pick color")
                }
            }
        },
    )
}

private fun String.toComposeColorOrNull(): Color? {
    return runCatching { Color(toColorInt()) }.getOrNull()
}

@Suppress("MagicNumber")
private fun Color.toSerializedColor(): String {
    return toArgb().toLong().and(0xffffffffL).toString(16).padStart(8, '0').uppercase().let { "#$it" }
}

@Composable
private fun <T> TextValue(
    definition: CheckoutPlaygroundSettingDefinition.Value<T>,
    value: String,
    enabled: Boolean,
    onValueChanged: (String) -> Unit,
) {
    val error = definition.validationError(value)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChanged,
        enabled = enabled,
        label = { Text(definition.displayName) },
        isError = error != null,
        keyboardOptions = KeyboardOptions(
            keyboardType = when (definition.input) {
                CheckoutPlaygroundSettingDefinition.Value.Input.Email -> KeyboardType.Email
                CheckoutPlaygroundSettingDefinition.Value.Input.Integer -> KeyboardType.Number
                CheckoutPlaygroundSettingDefinition.Value.Input.Decimal -> KeyboardType.Decimal
                else -> KeyboardType.Text
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(checkoutSettingValueTestTag(definition)),
    )
    error?.let {
        Text(text = it, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption)
    }
}

@Composable
private fun <T> RadioValue(
    definition: CheckoutPlaygroundSettingDefinition.Value<T>,
    value: String,
    enabled: Boolean,
    onValueChanged: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .testTag(checkoutSettingValueTestTag(definition))
            .semantics {
                if (!enabled) {
                    disabled()
                }
            }
            .alpha(if (enabled) 1f else ContentAlpha.disabled)
    ) {
        Text(text = definition.displayName, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth()) {
            definition.options.forEach { option ->
                Row(
                    modifier = Modifier
                        .selectable(
                            selected = value == definition.serialize(option.value),
                            enabled = enabled,
                            onClick = { onValueChanged(definition.serialize(option.value)) },
                        )
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = value == definition.serialize(option.value),
                        onClick = null,
                        enabled = enabled,
                    )
                    Text(option.displayName)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun <T> DropdownValue(
    definition: CheckoutPlaygroundSettingDefinition.Value<T>,
    value: String,
    enabled: Boolean,
    onValueChanged: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = definition.options.firstOrNull { definition.serialize(it.value) == value }
    LaunchedEffect(enabled) {
        if (!enabled) {
            expanded = false
        }
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = enabled && it },
        modifier = Modifier
            .testTag(checkoutSettingValueTestTag(definition))
            .semantics {
                if (!enabled) {
                    disabled()
                }
            },
    ) {
        OutlinedTextField(
            readOnly = true,
            enabled = enabled,
            value = selected?.displayName.orEmpty(),
            onValueChange = {},
            label = { Text(definition.displayName) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            definition.options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        onValueChanged(definition.serialize(option.value))
                        expanded = false
                    }
                ) {
                    Text(option.displayName)
                }
            }
        }
    }
}

internal fun checkoutSettingGroupTestTag(
    definition: CheckoutPlaygroundSettingDefinition.Configuration,
): String = CheckoutSettingGroupTestTagPrefix + definition.key

internal fun checkoutSettingValueTestTag(
    definition: CheckoutPlaygroundSettingDefinition.Value<*>,
): String = CheckoutSettingValueTestTagPrefix + definition.key

internal fun checkoutSettingBreadcrumbTestTag(
    definition: CheckoutPlaygroundSettingDefinition.Value<*>,
): String = CheckoutSettingBreadcrumbTestTagPrefix + definition.key

private const val MaxInlineOptions = 4
