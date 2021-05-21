package com.stripe.android.compose.elements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// TODO: The autocomplete needs some work make this to the filtering functionality.
@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun AutoComplete(
    items: List<String>,
    selectedItem: String,
    onValueChange: (String) -> Unit = {}
) {
    var dropDownVisible by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth(1f)

    ) {
        TextField(
            value = selectedItem,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth(1f)
                .clickable {
                    dropDownVisible = !dropDownVisible
                }
                .onFocusChanged {
                    dropDownVisible = it == FocusState.Active
                }
        )
        AnimatedVisibility(visible = dropDownVisible) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .border(
                        border = BorderStroke(2.dp, Color.Black),
                        shape = MaterialTheme.shapes.small.copy(
                            topEnd = ZeroCornerSize,
                            topStart = ZeroCornerSize,
                            bottomEnd = ZeroCornerSize,
                            bottomStart = ZeroCornerSize
                        )
                    ),
                horizontalAlignment = Alignment.Start
            ) {
                items(items) { item ->
                    Box(modifier = Modifier
                        .fillMaxWidth(1f)
                        .clickable {
                            onValueChange(item)
                            dropDownVisible = false
                        }) {
                        Text(
                            item, modifier = Modifier
                                .fillMaxWidth(1f)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
