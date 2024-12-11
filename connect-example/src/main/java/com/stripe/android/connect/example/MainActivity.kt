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
import com.stripe.android.connect.example.core.safeNavigateUp
import com.stripe.android.connect.example.ui.common.ConnectSdkExampleTheme
import com.stripe.android.connect.example.ui.componentpicker.ComponentPickerContent
import com.stripe.android.connect.example.ui.embeddedcomponentmanagerloader.EmbeddedComponentLoaderViewModel
import com.stripe.android.connect.example.ui.settings.SettingsView
import com.stripe.android.connect.example.ui.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@OptIn(PrivateBetaConnectSDK::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EmbeddedComponentManager.onActivityCreate(this@MainActivity)

        setContent {
            val viewModel = hiltViewModel<EmbeddedComponentLoaderViewModel>()
            val navController = rememberNavController()
            ConnectSdkExampleTheme {
                NavHost(navController = navController, startDestination = MainDestination.ComponentPicker) {
                    composable(MainDestination.ComponentPicker) {
                        ComponentPickerContent(
                            viewModel = viewModel,
                            openSettings = { navController.navigate(route = MainDestination.Settings) },
                        )
                    }
                    composable(MainDestination.Settings) {
                        val settingsViewModel = hiltViewModel<SettingsViewModel>()
                        SettingsView(
                            viewModel = settingsViewModel,
                            onDismiss = { navController.safeNavigateUp() },
                            onReloadRequested = viewModel::reload,
                        )
                    }
                }
            }
        }
    }
}

@Suppress("ConstPropertyName")
private object MainDestination {
    const val ComponentPicker = "ComponentPicker"
    const val Settings = "Settings"
}
