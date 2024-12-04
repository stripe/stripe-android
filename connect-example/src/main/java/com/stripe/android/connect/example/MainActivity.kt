package com.stripe.android.connect.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.data.EmbeddedComponentManagerProvider
import com.stripe.android.connect.example.ui.accountloader.EmbeddedComponentLoader
import com.stripe.android.connect.example.ui.accountloader.EmbeddedComponentLoaderViewModel
import com.stripe.android.connect.example.ui.componentpicker.ComponentPickerScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@OptIn(PrivateBetaConnectSDK::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: EmbeddedComponentLoaderViewModel by viewModels()

    @Inject
    lateinit var embeddedComponentManagerProvider: EmbeddedComponentManagerProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.state.collectAsState()
            val embeddedComponentAsync = state.embeddedComponentAsync
            ConnectSdkExampleTheme {
                EmbeddedComponentLoader(
                    embeddedComponentAsync = embeddedComponentAsync,
                    reload = viewModel::reload,
                ) {
                    ComponentPickerScreen(onReloadRequested = viewModel::reload)
                }
            }
        }
    }
}
