package com.stripe.android.paymentsheet.example.devtools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun DevToolsNetwork() {
    val endpoints = remember { DevToolsStore.endpoints }

    LazyColumn(
        modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection())
    ) {
        itemsIndexed(endpoints) { index, endpoint ->
            DevToolsEndpointItem(
                endpoint = endpoint,
                isLastItem = index == endpoints.lastIndex,
                onToggle = { DevToolsStore.toggleFailureFor(endpoint) }
            )
        }
    }
}

@Composable
private fun DevToolsEndpointItem(
    endpoint: Endpoint,
    isLastItem: Boolean,
    onToggle: () -> Unit
) {
    val isTurnedOn = endpoint in DevToolsStore.failingEndpoints

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val title = remember(endpoint.name) {
            endpoint.name.buildAnnotatedUrl()
        }

        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = isTurnedOn,
            onCheckedChange = { onToggle() }
        )
    }

    if (!isLastItem) {
        Divider()
    }
}

private fun String.buildAnnotatedUrl(): AnnotatedString {
    val parts = split("{", "}")
    val paramParts = parts.filterIndexed { index, _ -> index % 2 == 1 }

    return buildAnnotatedString {
        for (part in parts) {
            val color = if (part in paramParts) Color.Gray else Color.Unspecified
            withStyle(style = SpanStyle(color = color)) {
                append(part)
            }
        }
    }
}
