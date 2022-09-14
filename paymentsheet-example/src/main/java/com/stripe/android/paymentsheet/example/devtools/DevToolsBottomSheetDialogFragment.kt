package com.stripe.android.paymentsheet.example.devtools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class DevToolsBottomSheetDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                DevTools()
            }
        }
    }

    companion object {
        fun newInstance() = DevToolsBottomSheetDialogFragment()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DevTools() {
    val endpoints = remember { DevToolsStore.endpoints }
    val scrollState = rememberLazyListState()

    val hasScrolled = remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 1 || scrollState.firstVisibleItemScrollOffset > 0
        }
    }

    Column(modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection())) {
        Surface(
            elevation = if (hasScrolled.value) 8.dp else 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Dev Tools",
                style = MaterialTheme.typography.h5,
                modifier = Modifier.padding(16.dp)
            )
        }

        LazyColumn(state = scrollState) {
            item {
                Text(
                    text = "Fail requests to endpoints",
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            itemsIndexed(endpoints) { index, endpoint ->
                DevToolsEndpointItem(
                    endpoint = endpoint,
                    isLastItem = index == endpoints.lastIndex,
                    onToggle = { DevToolsStore.toggleFailureFor(endpoint) }
                )
            }
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
