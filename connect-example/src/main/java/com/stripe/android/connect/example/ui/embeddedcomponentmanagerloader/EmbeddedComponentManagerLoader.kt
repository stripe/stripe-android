package com.stripe.android.connect.example.ui.embeddedcomponentmanagerloader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.core.Async
import com.stripe.android.connect.example.core.Fail
import com.stripe.android.connect.example.core.Loading
import com.stripe.android.connect.example.core.Success
import com.stripe.android.connect.example.core.Uninitialized

/**
 * Manages the UI for loading an [EmbeddedComponentManager], including error states.
 * Calls [content] with a successful manager instance when available.
 */
@OptIn(PrivateBetaConnectSDK::class)
@Composable
fun EmbeddedComponentManagerLoader(
    embeddedComponentAsync: Async<EmbeddedComponentManager>,
    reload: () -> Unit,
    openSettings: () -> Unit,
    content: @Composable (embeddedComponentManager: EmbeddedComponentManager) -> Unit,
) {
    val embeddedComponentManager = embeddedComponentAsync()
    when (embeddedComponentAsync) {
        is Uninitialized -> {
            // Don't show anything to avoid flicker.
        }
        is Loading -> LoadingScreen()
        is Fail -> ErrorScreen(
            errorMessage = embeddedComponentAsync.error.message ?: stringResource(R.string.error_initializing),
            onReloadRequested = reload,
            openSettings = openSettings,
        )
        is Success -> {
            if (embeddedComponentManager != null) {
                content(embeddedComponentManager)
            } else {
                ErrorScreen(
                    errorMessage = stringResource(R.string.error_initializing),
                    onReloadRequested = reload,
                    openSettings = openSettings,
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.warming_up_the_server),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorScreen(
    errorMessage: String,
    onReloadRequested: () -> Unit,
    openSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.failed_to_start_app))
        TextButton(onClick = onReloadRequested) {
            Text(stringResource(R.string.reload))
        }
        TextButton(onClick = openSettings) {
            Text(stringResource(R.string.app_settings))
        }

        Text(errorMessage)
    }
}

// Previews

@OptIn(PrivateBetaConnectSDK::class)
@Preview(showBackground = true)
@Composable
private fun EmbeddedComponentLoaderLoadingPreview() {
    EmbeddedComponentManagerLoader(
        embeddedComponentAsync = Loading(),
        reload = {},
        openSettings = {},
        content = {},
    )
}

@OptIn(PrivateBetaConnectSDK::class)
@Preview(showBackground = true)
@Composable
private fun EmbeddedComponentLoaderErrorPreview() {
    EmbeddedComponentManagerLoader(
        embeddedComponentAsync = Fail(IllegalStateException("Example error")),
        reload = {},
        openSettings = {},
        content = {},
    )
}
