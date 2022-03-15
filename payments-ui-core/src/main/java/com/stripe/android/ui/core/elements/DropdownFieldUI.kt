package com.stripe.android.ui.core.elements

import DropdownMenu
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ContentAlpha
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
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.menu.DropdownMenuItemDefaultMaxWidth
import com.stripe.android.ui.core.elements.menu.DropdownMenuItemDefaultMinHeight
import com.stripe.android.ui.core.elements.menu.DropdownMenuItemDefaultMinWidth
import com.stripe.android.ui.core.elements.menu.DropdownMenuItemHorizontalPadding
import kotlin.math.max
import kotlin.math.min


@Composable
internal fun DropDown(
    controller: DropdownFieldController,
    enabled: Boolean,
) {
    val label by controller.label.collectAsState(
        null
    )
    val selectedIndex by controller.selectedIndex.collectAsState(0)
    val items = controller.displayItems
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val currentTextColor = if (enabled) {
        PaymentsTheme.colors.material.onBackground
    } else {
        TextFieldDefaults
            .textFieldColors()
            .indicatorColor(enabled, false, interactionSource)
            .value
    }

    val inputModeManager = LocalInputModeManager.current
    Box {
        Box(
            modifier = Modifier
                .focusProperties {
                    canFocus = inputModeManager.inputMode != InputMode.Touch
                }
                .clickable(
                    enabled = enabled,
                    onClickLabel = stringResource(R.string.change),
                ) {
                    expanded = true
                }
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 16.dp,
                    top = 4.dp,
                    bottom = 8.dp
                )
            ) {
                DropdownLabel(label, enabled)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        items[selectedIndex],
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
                .background(color = PaymentsTheme.colors.colorComponentBackground)
                .width(DropdownMenuItemDefaultMaxWidth)
                .requiredSizeIn(maxHeight = DropdownMenuItemDefaultMinHeight * 8.9f)
        ) {
            itemsIndexed(items) { index, displayValue ->
                DropdownMenuItem(
                    displayValue,
                    isSelected = index == selectedIndex,
                    currentTextColor,
                    onClick = {
                        expanded = false
                        controller.onValueChange(index)
                    }
                )
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
    @StringRes label: Int?,
    enabled: Boolean
) {
    val color = PaymentsTheme.colors.placeholderText
    label?.let {
        Text(
            stringResource(label),
            color = if (enabled) color else color.copy(alpha = ContentAlpha.disabled),
            modifier = Modifier.focusable(false),
            style = MaterialTheme.typography.caption
        )
    }
}

@Composable
internal fun DropdownMenuItem(
    displayValue: String,
    isSelected: Boolean,
    currentTextColor: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(
                horizontal = DropdownMenuItemHorizontalPadding,
            )
            .requiredSizeIn(
                minWidth = DropdownMenuItemDefaultMinWidth,
                minHeight = DropdownMenuItemDefaultMinHeight
            )
            .fillMaxWidth()
            .clickable {
                onClick()
            }
    ) {
        if (isSelected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier
                    .height(24.dp)
                    .padding(end = 4.dp),
                tint = PaymentsTheme.colors.material.primary
            )
        }
        Text(
            text = displayValue,
            color = if (isSelected) {
                PaymentsTheme.colors.material.primary
            } else {
                currentTextColor
            },
            fontWeight = if (isSelected) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            }
        )
    }
}
