package com.stripe.android.connect.example.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
@Preview
private fun SettingsNavigationItemPreview() {
    SettingsNavigationItem(text = "Settings", onClick = {})
}
