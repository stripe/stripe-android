package com.stripe.android.connect.example.ui.common

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
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
import com.stripe.android.connect.BuildConfig
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.data.EmbeddedComponentManagerProvider
import com.stripe.android.connect.example.ui.accountloader.EmbeddedComponentLoader
import com.stripe.android.connect.example.ui.accountloader.EmbeddedComponentLoaderViewModel
import com.stripe.android.connect.example.ui.appearance.AppearanceView
import com.stripe.android.core.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(PrivateBetaConnectSDK::class)
@AndroidEntryPoint
abstract class BasicExampleComponentActivity : FragmentActivity() {

    @Inject
    lateinit var embeddedComponentManagerProvider: EmbeddedComponentManagerProvider

    private val viewModel: EmbeddedComponentLoaderViewModel by viewModels()

    protected val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)

    @get:StringRes
    abstract val titleRes: Int

    abstract fun createComponentView(context: Context, embeddedComponentManager: EmbeddedComponentManager): View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BackHandler(onBack = ::finish)

            ConnectSdkExampleTheme {
                ExampleComponentContent()
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun ExampleComponentContent() {
        val state by viewModel.state.collectAsState()
        val embeddedComponentAsync = state.embeddedComponentAsync

        val sheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            skipHalfExpanded = true,
        )
        val coroutineScope = rememberCoroutineScope()

        ConnectExampleScaffold(
            title = stringResource(titleRes),
            navigationIcon = (embeddedComponentAsync is Success).then {
                { BackIconButton(onClick = ::finish) }
            },
            actions = (embeddedComponentAsync is Success).then {
                {
                    MoreIconButton(onClick = {
                        coroutineScope.launch {
                            if (!sheetState.isVisible) {
                                sheetState.show()
                            } else {
                                sheetState.hide()
                            }
                        }
                    })
                }
            } ?: { },
            modalSheetState = sheetState,
            modalContent = {
                AppearanceView(onDismiss = { coroutineScope.launch { sheetState.hide() } },)
            },
        ) {
            EmbeddedComponentLoader(
                embeddedComponentAsync = embeddedComponentAsync,
                reload = viewModel::reload,
            ) { embeddedComponentManager ->
                AndroidView(modifier = Modifier.fillMaxSize(), factory = { context ->
                    createComponentView(context, embeddedComponentManager)
                })
            }
        }
    }
}
