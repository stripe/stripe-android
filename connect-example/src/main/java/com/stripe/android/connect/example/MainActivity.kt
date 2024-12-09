package com.stripe.android.connect.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stripe.android.connect.example.ui.appearance.AppearanceView
import com.stripe.android.connect.example.ui.appearance.AppearanceViewModel
import com.stripe.android.connect.example.ui.common.ConnectSdkExampleTheme
import com.stripe.android.connect.example.ui.componentpicker.ComponentPickerContent
import com.stripe.android.connect.example.ui.embeddedcomponentmanagerloader.EmbeddedComponentLoaderViewModel
import com.stripe.android.connect.example.ui.settings.SettingsView
import com.stripe.android.connect.example.ui.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel = hiltViewModel<EmbeddedComponentLoaderViewModel>()
            val navController = rememberNavController()
            ConnectSdkExampleTheme {
                NavHost(navController = navController, startDestination = MainDestination.ComponentPicker) {
                    composable(MainDestination.ComponentPicker) {
                        ComponentPickerContent(
                            viewModel = viewModel,
                            openSettings = { navController.navigate(route = MainDestination.Settings) },
                            openAppearance = { navController.navigate(route = MainDestination.Appearance) },
                        )
                    }
                    composable(MainDestination.Settings) {
                        val settingsViewModel = hiltViewModel<SettingsViewModel>()
                        SettingsView(
                            viewModel = settingsViewModel,
                            onDismiss = { navController.popBackStack() },
                            onReloadRequested = viewModel::reload,
                        )
                    }
                    composable(MainDestination.Appearance) {
                        val appearanceViewModel = hiltViewModel<AppearanceViewModel>()
                        AppearanceView(
                            viewModel = appearanceViewModel,
                            onDismiss = { navController.popBackStack() },
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
    const val Appearance = "Appearance"
}
