package com.stripe.android.paymentsheet.elements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asLiveData

@Composable
internal fun SaveForFutureUseElementUI(
    enabled: Boolean,
    element: SaveForFutureUseElement
) {
    val controller = element.controller
    val checked by controller.saveForFutureUse.asLiveData()
        .observeAsState(true)
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        Checkbox(
            checked = checked,
            onCheckedChange = { controller.onValueChange(it) },
            enabled = enabled
        )
        Text(
            stringResource(controller.label, element.merchantName ?: ""),
            Modifier
                .padding(start = 4.dp)
                .align(Alignment.CenterVertically)
                .clickable(
                    enabled, null,
                    null
                ) {
                    controller.toggleValue()
                },
            color = if (isSystemInDarkTheme()) {
                Color.LightGray
            } else {
                Color.Black
            }
        )
    }
}
