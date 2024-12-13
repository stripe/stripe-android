package com.stripe.android.connect.example.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.connect.example.ui.common.ConnectSdkExampleTheme

@Composable
fun SettingsSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.h6,
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsSectionHeaderPreview() {
    SettingsSectionHeader("Settings")
}

@Composable
fun SettingsTextField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        value = value,
        onValueChange = onValueChange,
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsTextFieldPreview() {
    ConnectSdkExampleTheme {
        Column {
            SettingsTextField(
                label = "Name",
                placeholder = "Jane Doe",
                value = "",
                onValueChange = {}
            )
            SettingsTextField(
                label = "Name",
                placeholder = "Jane Doe",
                value = "John",
                onValueChange = {}
            )
        }
    }
}

@Composable
fun SettingsNavigationItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            fontSize = 16.sp,
        )
        Icon(
            modifier = Modifier
                .size(36.dp)
                .padding(start = 8.dp),
            contentDescription = null,
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun SettingsNavigationItemPreview() {
    SettingsNavigationItem(text = "Settings", onClick = {})
}

@Suppress("LongMethod")
@Composable
fun <T> SettingsDropdownField(
    label: String,
    options: List<T>,
    selectedOption: T,
    onSelectOption: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionToString: (T) -> String = { it.toString() },
) {
    var isExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .padding(
                start = 16.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .weight(1f),
            text = label,
            color = MaterialTheme.colors.onSurface,
        )
        Box(
            modifier = Modifier
                .clickable { isExpanded = true }
                .padding(4.dp)
        ) {
            Row {
                Text(
                    text = optionToString(selectedOption),
                    color = MaterialTheme.colors.onSurface,
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    tint = MaterialTheme.colors.onSurface,
                    contentDescription = null,
                )
            }
            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        onClick = {
                            onSelectOption(option)
                            isExpanded = false
                        },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = optionToString(option))
                            Spacer(
                                Modifier
                                    .sizeIn(minWidth = 8.dp)
                                    .weight(1f)
                            )
                            val iconSize = 16.dp
                            if (option == selectedOption) {
                                Icon(
                                    modifier = Modifier.size(iconSize),
                                    imageVector = Icons.Default.Check,
                                    tint = MaterialTheme.colors.onSurface,
                                    contentDescription = null,
                                )
                            } else {
                                Spacer(modifier = Modifier.size(iconSize))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsDropdownFieldPreview() {
    ConnectSdkExampleTheme {
        SettingsDropdownField(
            label = "Label",
            options = listOf("Option 1", "Option 2"),
            selectedOption = "Option 1",
            onSelectOption = {},
            optionToString = { it },
        )
    }
}
