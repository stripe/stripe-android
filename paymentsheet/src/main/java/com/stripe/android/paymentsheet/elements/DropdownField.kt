package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asLiveData

@Composable
internal fun DropDown(
    @StringRes label: Int,
    controller: DropdownFieldController,
    enabled: Boolean,
) {
    val selectedIndex by controller.selectedIndex.asLiveData().observeAsState(0)
    val items = controller.displayItems
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val currentTextColor = if (enabled) {
        if (isSystemInDarkTheme()) {
            Color.White
        } else {
            Color.Unspecified
        }
    } else {
        TextFieldDefaults.textFieldColors().indicatorColor(enabled, false, interactionSource).value
    }

    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.TopStart)
            .background(Color.Transparent)

    ) {
        Box {
            Column(
                modifier = Modifier.padding(
                    start = 16.dp,
                    top = 4.dp,
                    bottom = 8.dp
                )
            ) {
                DropdownLabel(label)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        items[selectedIndex],
                        modifier = Modifier
                            .fillMaxWidth(.9f)
                            .clickable(
                                enabled = enabled,
                                onClick = { expanded = true }
                            ),
                        color = currentTextColor
                    )
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = "Dropdown arrow",
                        modifier = Modifier
                            .height(24.dp)
                            .clickable(onClick = { expanded = true }),
                        tint = currentTextColor
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEachIndexed { index, displayValue ->
                DropdownMenuItem(
                    onClick = {
                        controller.onValueChange(index)
                        expanded = false
                    }
                ) {
                    Text(text = displayValue)
                }
            }
        }
    }
}

/**
 * This will create the label for the DropdownTextField.
 *
 * Copied logic from androidx.compose.material.TextFieldImpl
 */
@Composable
internal fun DropdownLabel(
    @StringRes label: Int,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        stringResource(label),
        color = TextFieldDefaults.textFieldColors()
            .labelColor(
                enabled = true,
                error = false,
                interactionSource = interactionSource
            ).value,
        style = MaterialTheme.typography.caption
    )
}
