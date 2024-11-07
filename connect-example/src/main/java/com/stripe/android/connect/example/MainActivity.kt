package com.stripe.android.connect.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.connect.example.ui.componentpicker.ComponentPickerScreen
import com.stripe.android.connect.example.ui.settings.SettingsView
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = viewModel()
            val state by viewModel.state.collectAsState()

            ConnectSdkExampleTheme {
                when {
                    state.isLoading -> LoadingScreen()
                    state.errorMessage != null -> ErrorScreen(
                        errorMessage = state.errorMessage,
                        onReloadRequested = viewModel::reload,
                    )
                    else -> ComponentPickerScreen(
                        onReloadRequested = viewModel::reload,
                    )
                }
            }
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
        errorMessage: String? = null,
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

                    if (errorMessage != null) {
                        Text(errorMessage)
                    }
                }
            }
        }
    }
}
