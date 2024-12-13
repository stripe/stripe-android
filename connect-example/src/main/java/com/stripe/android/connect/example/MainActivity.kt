package com.stripe.android.connect.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.example.ui.common.ConnectSdkExampleTheme
import com.stripe.android.connect.example.ui.componentpicker.ComponentPickerContent
import com.stripe.android.connect.example.ui.embeddedcomponentmanagerloader.EmbeddedComponentLoaderViewModel
import com.stripe.android.connect.example.ui.settings.SettingsDestination
import com.stripe.android.connect.example.ui.settings.settingsComposables
import dagger.hilt.android.AndroidEntryPoint

@OptIn(PrivateBetaConnectSDK::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EmbeddedComponentManager.onActivityCreate(this@MainActivity)

        setContent {
            val viewModel = hiltViewModel<EmbeddedComponentLoaderViewModel>(this@MainActivity)
            val navController = rememberNavController()
            ConnectSdkExampleTheme {
                NavHost(navController = navController, startDestination = MainDestination.ComponentPicker) {
                    composable(MainDestination.ComponentPicker) {
                        ComponentPickerContent(
                            viewModel = viewModel,
                            openSettings = { navController.navigate(SettingsDestination.Settings) },
                        )
                    }
                    settingsComposables(this@MainActivity, navController)
                }
            }
        }
    }
}

@Suppress("ConstPropertyName")
private object MainDestination {
    const val ComponentPicker = "ComponentPicker"
}
