package com.stripe.android.paymentsheet.example.devtools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
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

@Composable
private fun DevTools() {
    val endpoints = remember { DevToolsStore.endpoints }

    LaunchedEffect(Unit) {
        DevToolsStore.loadEndpoints()
    }

    if (endpoints.isEmpty()) {
        DevToolsLoading()
    } else {
        DevToolsContent(endpoints = endpoints)
    }
}

@Composable
private fun DevToolsLoading() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun DevToolsContent(endpoints: List<String>) {
    Column {
        Text(
            text = "Dev Tools",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(16.dp)
        )

        Text(
            text = "Fail requests to endpoints",
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(16.dp)
        )

        for ((index, endpoint) in endpoints.withIndex()) {
            DevToolsEndpointItem(
                endpoint = endpoint,
                isLastItem = index == endpoints.lastIndex,
                onToggle = { DevToolsStore.toggle(endpoint) }
            )
        }
    }
}

@Composable
private fun DevToolsEndpointItem(
    endpoint: String,
    isLastItem: Boolean,
    onToggle: () -> Unit
) {
    val text = endpoint.removePrefix("https://api.stripe.com/v1/")
    val isTurnedOn = endpoint in DevToolsStore.failingEndpoints

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
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
