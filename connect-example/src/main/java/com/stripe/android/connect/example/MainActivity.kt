package com.stripe.android.connect.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.data.EmbeddedComponentManagerProvider
import com.stripe.android.connect.example.ui.accountloader.AccountLoaderScreen
import com.stripe.android.connect.example.ui.accountloader.AccountLoaderViewModel
import com.stripe.android.connect.example.ui.componentpicker.ComponentPickerScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@OptIn(PrivateBetaConnectSDK::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: AccountLoaderViewModel by viewModels()

    @Inject
    lateinit var embeddedComponentManagerProvider: EmbeddedComponentManagerProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ConnectSdkExampleTheme {
                AccountLoaderScreen(viewModel, embeddedComponentManagerProvider) { _ ->
                    ComponentPickerScreen(onReloadRequested = viewModel::reload)
                }
            }
        }
    }
}
