package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stripe.android.uicore.R
import com.stripe.android.uicore.stripeColors

@Preview
@Composable
private fun DropDownPreview() {
    Dropdown(
        controller = DropdownFieldController(
            CountryConfig(tinyMode = true)
        ),
        enabled = true
    )
}

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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Dropdown(
    controller: DropdownFieldController,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val label by controller.label.collectAsState(null)
    val selectedIndex by controller.selectedIndex.collectAsState(0)
    val items = controller.displayItems
    val shouldDisableDropdownWithSingleItem =
        items.count() == 1 && controller.disableDropdownWithSingleElement

    val shouldEnable = enabled && !shouldDisableDropdownWithSingleItem

    var expanded by remember { mutableStateOf(false) }
    val selectedItemLabel = controller.getSelectedItemLabel(selectedIndex)
    val interactionSource = remember { MutableInteractionSource() }
    val currentTextColor = if (shouldEnable) {
        MaterialTheme.stripeColors.onComponent
    } else {
        TextFieldDefaults
            .textFieldColors()
            .indicatorColor(enabled = false, isError = false, interactionSource = interactionSource)
            .value
    }

    val inputModeManager = LocalInputModeManager.current

    Box(
        modifier = modifier
            .wrapContentSize(Alignment.TopStart)
            .background(MaterialTheme.stripeColors.component)
    ) {
        // Click handling happens on the box, so that it is a single accessible item
        Box(
            modifier = Modifier
                .focusProperties {
                    canFocus = inputModeManager.inputMode != InputMode.Touch
                }
                .clickable(
                    enabled = shouldEnable,
                    onClickLabel = stringResource(R.string.stripe_change)
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
                    if (!shouldDisableDropdownWithSingleItem) {
                        Icon(
                            painter = painterResource(id = R.drawable.stripe_ic_chevron_down),
                            contentDescription = null,
                            modifier = Modifier.height(24.dp),
                            tint = MaterialTheme.stripeColors.placeholderText
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            top = 4.dp,
                            bottom = 8.dp
                        )
                    ) {
                        label?.let {
                            FormLabel(stringResource(it), enabled = shouldEnable)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(.9f),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                selectedItemLabel,
                                color = currentTextColor
                            )
                        }
                    }
                    if (!shouldDisableDropdownWithSingleItem) {
                        Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                            Icon(
                                painter = painterResource(id = R.drawable.stripe_ic_chevron_down),
                                contentDescription = null,
                                modifier = Modifier.height(24.dp),
                                tint = currentTextColor
                            )
                        }
                    }
                }
            }
        }

        if (expanded) {
            Dialog(
                onDismissRequest = { expanded = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                )
            ) {
                Surface(
                    elevation = 8.dp,
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colors.surface,
                    modifier = Modifier.padding(16.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .requiredSizeIn(maxHeight = DropdownMenuItemDefaultMinHeight * 8.9f)
                    ) {
                        itemsIndexed(items) { index, item ->
                            DropdownMenuItem(
                                displayValue = item,
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .requiredSizeIn(minHeight = DropdownMenuItemDefaultMinHeight)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = displayValue,
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

        Spacer(modifier = Modifier.weight(1f))

        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.height(24.dp),
                tint = MaterialTheme.colors.primary
            )
        }
    }
}

// Size defaults.
internal val DropdownMenuItemDefaultMinHeight = 48.dp
