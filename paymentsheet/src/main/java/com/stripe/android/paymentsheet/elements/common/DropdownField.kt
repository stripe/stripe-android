package com.stripe.android.paymentsheet.elements.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asLiveData

@Composable
internal fun DropDown(
    controller: DropdownFieldController,
) {
    val selectedIndex by controller.selectedIndex.asLiveData().observeAsState(0)
    val items = controller.displayItems
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.TopStart)
            .background(Color.Transparent)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                items[selectedIndex],
                modifier = Modifier
                    .fillMaxWidth(.9f)
                    .clickable(onClick = { expanded = true })
                    .padding(16.dp)
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = "Localized description"
            )
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
