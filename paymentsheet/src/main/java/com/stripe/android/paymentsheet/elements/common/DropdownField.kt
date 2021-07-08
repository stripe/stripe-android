package com.stripe.android.paymentsheet.elements.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.asLiveData

@Composable
internal fun DropDown(
    label: Int,
    controller: DropdownFieldController,
) {
    val selectedIndex by controller.selectedIndex.asLiveData().observeAsState(0)
    val items = controller.displayItems
    var expanded by remember { mutableStateOf(false) }

    val stringVal: (String) -> Unit = { }
    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.TopStart)
            .background(Color.Transparent)
    ) {
        Box {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {

//                androidx.compose.material.TextField(
//                    value = items[selectedIndex] as String,
//                    onValueChanged = stringVal as (String) -> Unit
//                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = "Localized description",
                    modifier = Modifier
                        .clickable(onClick = { expanded = true })
                )
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

@Composable
fun DropdownLabel(
    label: Int
) {
    val interactionSource = remember { MutableInteractionSource() }
    val labelAnimatedStyle = lerp(
        MaterialTheme.typography.subtitle1,
        MaterialTheme.typography.caption,
        .5f
    )
    Decoration(
        contentColor = TextFieldDefaults.textFieldColors()
            .labelColor(
                false,
                false,
                interactionSource
            ).value,
        typography = labelAnimatedStyle,
        content = {
            Text(
                stringResource(label),
                modifier = Modifier.padding(start = 16.dp),
                color = Color.Gray,
                fontSize = 12.sp,
            )
        }
    )
}

/**
 * Set content color, typography and emphasis for [content] composable
 */
@Composable
internal fun Decoration(
    contentColor: Color,
    typography: TextStyle? = null,
    contentAlpha: Float? = null,
    content: @Composable () -> Unit
) {
    val colorAndEmphasis: @Composable () -> Unit = @Composable {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            if (contentAlpha != null) {
                CompositionLocalProvider(
                    LocalContentAlpha provides contentAlpha,
                    content = content
                )
            } else {
                CompositionLocalProvider(
                    LocalContentAlpha provides contentColor.alpha,
                    content = content
                )
            }
        }
    }
    if (typography != null) ProvideTextStyle(typography, colorAndEmphasis) else colorAndEmphasis()
}
