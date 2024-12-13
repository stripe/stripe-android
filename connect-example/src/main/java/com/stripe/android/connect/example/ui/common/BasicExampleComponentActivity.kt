package com.stripe.android.connect.example.ui.common

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.core.Async
import com.stripe.android.connect.example.core.Success
import com.stripe.android.connect.example.core.then
import com.stripe.android.connect.example.ui.appearance.AppearanceView
import com.stripe.android.connect.example.ui.appearance.AppearanceViewModel
import com.stripe.android.connect.example.ui.embeddedcomponentmanagerloader.EmbeddedComponentLoaderViewModel
import com.stripe.android.connect.example.ui.embeddedcomponentmanagerloader.EmbeddedComponentManagerLoader
import com.stripe.android.connect.example.ui.embeddedcomponentmanagerloader.EmbeddedComponentManagerLoaderState
import com.stripe.android.connect.example.ui.settings.SettingsViewModel
import com.stripe.android.connect.example.ui.settings.settingsComposables
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@Suppress("ConstPropertyName")
private object BasicComponentExampleDestination {
    const val Component = "Component"
    const val Settings = "Settings"
}

@OptIn(PrivateBetaConnectSDK::class)
@AndroidEntryPoint
abstract class BasicExampleComponentActivity : FragmentActivity() {

    @get:StringRes
    abstract val titleRes: Int

    private val settingsViewModel by viewModels<SettingsViewModel>()

    abstract fun createComponentView(context: Context, embeddedComponentManager: EmbeddedComponentManager): View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EmbeddedComponentManager.onActivityCreate(this@BasicExampleComponentActivity)

        val settings = settingsViewModel.state.value
        val edgeToEdgeEnabled = settings.presentationSettings.edgeToEdgeEnabled
        if (edgeToEdgeEnabled) {
            enableEdgeToEdge()
        }

        setContent {
            BackHandler(onBack = ::finish)
            val viewModel = hiltViewModel<EmbeddedComponentLoaderViewModel>(this@BasicExampleComponentActivity)
            val navController = rememberNavController()
            ConnectSdkExampleTheme {
                NavHost(navController = navController, startDestination = BasicComponentExampleDestination.Component) {
                    composable(BasicComponentExampleDestination.Component) {
                        ExampleComponentContent(
                            viewModel = viewModel,
                            edgeToEdgeEnabled = edgeToEdgeEnabled,
                            openSettings = { navController.navigate(BasicComponentExampleDestination.Settings) },
                        )
                    }
                    settingsComposables(this@BasicExampleComponentActivity, navController)
                }
            }
        }
    }

    @Composable
    private fun ExampleComponentContent(
        viewModel: EmbeddedComponentLoaderViewModel,
        edgeToEdgeEnabled: Boolean,
        openSettings: () -> Unit,
    ) {
        val state by viewModel.state.collectAsState()
        val embeddedComponentAsync = state.embeddedComponentManagerAsync

        if (edgeToEdgeEnabled) {
            // don't render the scaffold if edge-to-edge is enabled so that we get an entirely
            // full-screen experience
            ExampleComponentView(
                embeddedComponentManagerAsync = embeddedComponentAsync,
                openSettings = openSettings,
                reload = viewModel::reload,
            )
        } else {
            ExampleComponentScaffold(
                embeddedComponentManagerAsync = embeddedComponentAsync,
            ) {
                ExampleComponentView(
                    embeddedComponentManagerAsync = embeddedComponentAsync,
                    openSettings = openSettings,
                    reload = viewModel::reload,
                )
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun ExampleComponentScaffold(
        embeddedComponentManagerAsync: Async<EmbeddedComponentManager>,
        content: @Composable () -> Unit,
    ) {
        val sheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            skipHalfExpanded = true,
        )
        val coroutineScope = rememberCoroutineScope()

        ModalBottomSheetLayout(
            modifier = Modifier.fillMaxSize(),
            sheetState = sheetState,
            sheetContent = {
                val appearanceViewModel = hiltViewModel<AppearanceViewModel>()
                AppearanceView(
                    viewModel = appearanceViewModel,
                    onDismiss = { coroutineScope.launch { sheetState.hide() } },
                )
            },
        ) {
            ConnectExampleScaffold(
                title = stringResource(titleRes),
                navigationIcon = (embeddedComponentManagerAsync is Success).then {
                    {
                        BackIconButton(onClick = ::finish)
                    }
                },
                actions = (embeddedComponentManagerAsync is Success).then {
                    {
                        CustomizeAppearanceIconButton(onClick = { coroutineScope.launch { sheetState.show() } })
                    }
                } ?: { },
                content = content
            )
        }
    }

    @Composable
    private fun ExampleComponentView(
        embeddedComponentManagerAsync: Async<EmbeddedComponentManager>,
        openSettings: () -> Unit,
        reload: () -> Unit,
    ) {
        EmbeddedComponentManagerLoader(
            embeddedComponentAsync = embeddedComponentManagerAsync,
            openSettings = openSettings,
            reload = reload,
        ) { embeddedComponentManager ->
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { context ->
                createComponentView(context, embeddedComponentManager)
            })
        }
    }
}
