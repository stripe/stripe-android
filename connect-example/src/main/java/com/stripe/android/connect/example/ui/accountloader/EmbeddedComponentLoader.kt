package com.stripe.android.connect.example.ui.accountloader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.ConnectExampleScaffold
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.ui.common.Async
import com.stripe.android.connect.example.ui.common.Fail
import com.stripe.android.connect.example.ui.common.Loading
import com.stripe.android.connect.example.ui.common.Success
import com.stripe.android.connect.example.ui.common.Uninitialized
import com.stripe.android.connect.example.ui.settings.SettingsView
import kotlinx.coroutines.launch

/**
 * Manages the UI for loading an [EmbeddedComponentManager], including error states.
 * Calls [content] with a successful manager instance when available.
 */
@OptIn(PrivateBetaConnectSDK::class)
@Composable
fun EmbeddedComponentLoader(
    embeddedComponentAsync: Async<EmbeddedComponentManager>,
    reload: () -> Unit,
    content: @Composable (embeddedComponentManager: EmbeddedComponentManager) -> Unit,
) {
    val embeddedComponentManager = embeddedComponentAsync()
    when (embeddedComponentAsync) {
        is Uninitialized, is Loading -> LoadingScreen()
        is Fail -> ErrorScreen(
            errorMessage = embeddedComponentAsync.error.message ?: stringResource(R.string.error_initializing),
            onReloadRequested = reload,
        )
        is Success -> {
            if (embeddedComponentManager != null) {
                content(embeddedComponentManager)
            } else {
                ErrorScreen(
                    errorMessage = stringResource(R.string.error_initializing),
                    onReloadRequested = reload,
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.warming_up_the_server),
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ErrorScreen(
    errorMessage: String,
    onReloadRequested: () -> Unit,
) {
    val settingsSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
    )
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheetLayout(
        modifier = Modifier.fillMaxSize(),
        sheetState = settingsSheetState,
        sheetContent = {
            SettingsView(
                onDismiss = { coroutineScope.launch { settingsSheetState.hide() } },
                onReloadRequested = onReloadRequested,
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.failed_to_start_app))
            TextButton(onClick = onReloadRequested) {
                Text(stringResource(R.string.reload))
            }
            TextButton(onClick = {
                coroutineScope.launch {
                    if (!settingsSheetState.isVisible) {
                        settingsSheetState.show()
                    } else {
                        settingsSheetState.hide()
                    }
                }
            }) {
                Text(stringResource(R.string.app_settings))
            }

            Text(errorMessage)
        }
    }
}

// Previews

@OptIn(PrivateBetaConnectSDK::class)
@Preview(showBackground = true)
@Composable
fun EmbeddedComponentLoaderLoadingPreview() {
    EmbeddedComponentLoader(
        embeddedComponentAsync = Loading(),
        reload = { },
        content = { },
    )
}

@OptIn(PrivateBetaConnectSDK::class)
@Preview(showBackground = true)
@Composable
fun EmbeddedComponentLoaderErrorPreview() {
    EmbeddedComponentLoader(
        embeddedComponentAsync = Fail(IllegalStateException("Example error")),
        reload = { },
        content = { },
    )
}
