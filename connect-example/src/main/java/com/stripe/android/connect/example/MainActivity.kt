package com.stripe.android.connect.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.ui.embeddedcomponentmanagerloader.EmbeddedComponentManagerLoader
import com.stripe.android.connect.example.ui.embeddedcomponentmanagerloader.EmbeddedComponentLoaderViewModel
import com.stripe.android.connect.example.ui.appearance.AppearanceView
import com.stripe.android.connect.example.ui.common.ConnectExampleScaffold
import com.stripe.android.connect.example.ui.common.ConnectSdkExampleTheme
import com.stripe.android.connect.example.ui.common.Success
import com.stripe.android.connect.example.ui.common.then
import com.stripe.android.connect.example.ui.componentpicker.ComponentPickerList
import com.stripe.android.connect.example.ui.settings.SettingsView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@OptIn(PrivateBetaConnectSDK::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: EmbeddedComponentLoaderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ConnectSdkExampleTheme {
                ComponentPickerContent()
            }
        }
    }

    @Suppress("LongMethod")
    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun ComponentPickerContent() {
        val state by viewModel.state.collectAsState()
        val embeddedComponentAsync = state.embeddedComponentAsync

        val sheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            skipHalfExpanded = true,
        )
        var sheetType by rememberSaveable { mutableStateOf(SheetType.SETTINGS) }
        val coroutineScope = rememberCoroutineScope()

        ConnectExampleScaffold(
            title = stringResource(R.string.connect_sdk_example),
            actions = (embeddedComponentAsync is Success).then {
                {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                if (!sheetState.isVisible) {
                                    sheetType = SheetType.SETTINGS
                                    sheetState.show()
                                } else {
                                    sheetState.hide()
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                if (!sheetState.isVisible) {
                                    sheetType = SheetType.APPEARANCE
                                    sheetState.show()
                                } else {
                                    sheetState.hide()
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.customize_appearance),
                        )
                    }
                }
            } ?: { },
            modalSheetState = sheetState,
            modalContent = (embeddedComponentAsync is Success).then {
                {
                    BackHandler(enabled = sheetState.isVisible) {
                        coroutineScope.launch { sheetState.hide() }
                    }
                    when (sheetType) {
                        SheetType.SETTINGS -> SettingsView(
                            onDismiss = { coroutineScope.launch { sheetState.hide() } },
                            onReloadRequested = viewModel::reload,
                        )
                        SheetType.APPEARANCE -> AppearanceView(
                            onDismiss = { coroutineScope.launch { sheetState.hide() } },
                        )
                    }
                }
            },
        ) {
            EmbeddedComponentManagerLoader(
                embeddedComponentAsync = embeddedComponentAsync,
                reload = viewModel::reload,
            ) {
                ComponentPickerList()
            }
        }
    }

    private enum class SheetType {
        SETTINGS,
        APPEARANCE,
    }
}
