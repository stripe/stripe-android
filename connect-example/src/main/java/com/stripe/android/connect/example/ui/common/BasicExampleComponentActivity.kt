package com.stripe.android.connect.example.ui.common

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.core.Success
import com.stripe.android.connect.example.core.then
import com.stripe.android.connect.example.data.SettingsService
import com.stripe.android.connect.example.ui.appearance.AppearanceInfo
import com.stripe.android.connect.example.ui.appearance.AppearanceView
import com.stripe.android.connect.example.ui.appearance.AppearanceViewModel
import com.stripe.android.connect.example.ui.embeddedcomponentmanagerloader.EmbeddedComponentLoaderViewModel
import com.stripe.android.connect.example.ui.embeddedcomponentmanagerloader.EmbeddedComponentManagerLoader
import com.stripe.android.connect.example.ui.settings.settingsComposables
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    abstract fun createComponentView(context: Context, embeddedComponentManager: EmbeddedComponentManager): View

    @Inject
    lateinit var settingsService: SettingsService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EmbeddedComponentManager.onActivityCreate(this@BasicExampleComponentActivity)

        setContent {
            BackHandler(onBack = ::finish)
            val viewModel = hiltViewModel<EmbeddedComponentLoaderViewModel>(this@BasicExampleComponentActivity)
            val navController = rememberNavController()
            ConnectSdkExampleTheme {
                NavHost(navController = navController, startDestination = BasicComponentExampleDestination.Component) {
                    composable(BasicComponentExampleDestination.Component) {
                        ExampleComponentContent(
                            viewModel = viewModel,
                            openSettings = { navController.navigate(BasicComponentExampleDestination.Settings) },
                        )
                    }
                    settingsComposables(this@BasicExampleComponentActivity, navController)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun ExampleComponentContent(
        viewModel: EmbeddedComponentLoaderViewModel,
        openSettings: () -> Unit,
    ) {
        val state by viewModel.state.collectAsState()
        val embeddedComponentAsync = state.embeddedComponentManagerAsync

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
                navigationIcon = (embeddedComponentAsync is Success).then {
                    {
                        BackIconButton(onClick = ::finish)
                    }
                },
                actions = (embeddedComponentAsync is Success).then {
                    {
                        CustomizeAppearanceIconButton(onClick = { coroutineScope.launch { sheetState.show() } })
                    }
                } ?: { },
            ) {
                EmbeddedComponentManagerLoader(
                    embeddedComponentAsync = embeddedComponentAsync,
                    openSettings = openSettings,
                    reload = viewModel::reload,
                ) { embeddedComponentManager ->
                    val context = LocalContext.current
                    LaunchedEffect(context) {
                        val appearanceInfo = settingsService.getAppearanceId()
                            ?.let { AppearanceInfo.getAppearance(it, context).appearance }
                            ?: return@LaunchedEffect
                        embeddedComponentManager.update(appearanceInfo)
                    }
                    AndroidView(modifier = Modifier.fillMaxSize(), factory = {
                        createComponentView(it, embeddedComponentManager)
                    })
                }
            }
        }
    }
}
