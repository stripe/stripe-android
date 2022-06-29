package com.stripe.android.ui.core.elements

import DropdownMenu
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.menu.DropdownMenuItemDefaultMaxWidth
import com.stripe.android.ui.core.elements.menu.DropdownMenuItemDefaultMinHeight
import com.stripe.android.ui.core.paymentsColors
import kotlin.math.max
import kotlin.math.min

/**
 * This composable will handle the display of dropdown items
 * in a lazy column.
 *
 * Here are some relevant manual tests:
 *   - Short list of dropdown items
 *   - long list of dropdown items
 *   - Varying width of dropdown item
 *   - Display setting very large
 *   - Whole row is clickable, not just text
 *   - Scrolls to the selected item in the list
 */
@Composable
internal fun DropDown(
    controller: DropdownFieldController,
    enabled: Boolean
) {
    val label by controller.label.collectAsState(null)
    val selectedIndex by controller.selectedIndex.collectAsState(0)
    val items = controller.displayItems
    var expanded by remember { mutableStateOf(false) }
    val selectedItemLabel = controller.getSelectedItemLabel(selectedIndex)
    val interactionSource = remember { MutableInteractionSource() }
    val currentTextColor = if (enabled) {
        MaterialTheme.paymentsColors.onComponent
    } else {
        TextFieldDefaults
            .textFieldColors()
            .indicatorColor(enabled, false, interactionSource)
            .value
    }

    val inputModeManager = LocalInputModeManager.current
    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.TopStart)
            .background(MaterialTheme.paymentsColors.component)
    ) {
        // Click handling happens on the box, so that it is a single accessible item
        Box(
            modifier = Modifier
                .focusProperties {
                    canFocus = inputModeManager.inputMode != InputMode.Touch
                }
                .clickable(
                    enabled = enabled,
                    onClickLabel = stringResource(R.string.change)
                ) {
                    expanded = true
                }
        ) {
            if (controller.tinyMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        selectedItemLabel,
                        color = currentTextColor
                    )
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = currentTextColor
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 4.dp,
                        bottom = 8.dp
                    )
                ) {
                    label?.let {
                        FormLabel(stringResource(it), enabled = enabled)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            selectedItemLabel,
                            modifier = Modifier.fillMaxWidth(.9f),
                            color = currentTextColor
                        )
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.height(24.dp),
                            tint = currentTextColor
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = expanded,

            // We will show up to two items before
            initialFirstVisibleItemIndex = if (selectedIndex >= 1) {
                min(
                    max(selectedIndex - 2, 0),
                    max(selectedIndex - 1, 0)
                )
            } else {
                selectedIndex
            },
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(color = MaterialTheme.paymentsColors.component)
                .width(DropdownMenuItemDefaultMaxWidth)
                .requiredSizeIn(maxHeight = DropdownMenuItemDefaultMinHeight * 8.9f)
        ) {
            itemsIndexed(items) { index, displayValue ->
                DropdownMenuItem(
                    displayValue = displayValue,
                    isSelected = index == selectedIndex,
                    currentTextColor = currentTextColor,
                    onClick = {
                        expanded = false
                        controller.onValueChange(index)
                    }
                )
            }
        }
    }
}

@Composable
internal fun DropdownMenuItem(
    displayValue: String,
    isSelected: Boolean,
    currentTextColor: Color,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .requiredSizeIn(
                minHeight = DropdownMenuItemDefaultMinHeight
            )
            .clickable {
                onClick()
            }
    ) {
        Text(
            text = displayValue,
            modifier = Modifier
                // This padding makes up for the checkmark at the end.
                .padding(
                    start = 13.dp
                )
                .fillMaxWidth(.8f),
            color = if (isSelected) {
                MaterialTheme.colors.primary
            } else {
                currentTextColor
            },
            fontWeight = if (isSelected) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            }
        )

        if (isSelected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier
                    .height(24.dp),
                tint = MaterialTheme.colors.primary
            )
        }
    }
}
