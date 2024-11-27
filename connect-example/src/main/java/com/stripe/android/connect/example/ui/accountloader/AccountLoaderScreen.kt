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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.MainContent
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.data.EmbeddedComponentManagerProvider
import com.stripe.android.connect.example.ui.settings.SettingsView
import kotlinx.coroutines.launch

@OptIn(PrivateBetaConnectSDK::class)
@Composable
fun AccountLoaderScreen(
    viewModel: AccountLoaderViewModel,
    embeddedComponentManagerProvider: EmbeddedComponentManagerProvider,
    content: @Composable (embeddedComponentManager: EmbeddedComponentManager) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val embeddedComponentManager: EmbeddedComponentManager? by remember {
        derivedStateOf {
            if (!state.isLoading && state.errorMessage == null) {
                embeddedComponentManagerProvider.provideEmbeddedComponentManager()
            } else {
                null
            }
        }
    }
    AccountLoader(
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
        embeddedComponentManager = embeddedComponentManager,
        reload = viewModel::reload,
        content = content,
    )
}

@OptIn(PrivateBetaConnectSDK::class)
@Composable
private fun AccountLoader(
    isLoading: Boolean,
    errorMessage: String?,
    embeddedComponentManager: EmbeddedComponentManager?,
    reload: () -> Unit,
    content: @Composable (embeddedComponentManager: EmbeddedComponentManager) -> Unit,
) {
    when {
        isLoading -> LoadingScreen()
        errorMessage != null || embeddedComponentManager == null -> ErrorScreen(
            errorMessage = errorMessage ?: stringResource(R.string.error_initializing),
            onReloadRequested = reload,
        )
        else -> content(embeddedComponentManager)
    }
}

@Composable
private fun LoadingScreen() {
    MainContent(title = stringResource(R.string.connect_sdk_example)) {
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
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ErrorScreen(
    errorMessage: String,
    onReloadRequested: () -> Unit,
) {
    MainContent(
        title = stringResource(R.string.connect_sdk_example),
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
}