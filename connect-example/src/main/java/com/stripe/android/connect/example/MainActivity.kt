package com.stripe.android.connect.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stripe.android.connect.example.ui.common.ConnectSdkExampleTheme
import com.stripe.android.connect.example.ui.componentpicker.ComponentPickerContent
import com.stripe.android.connect.example.ui.settings.SettingsDestination
import com.stripe.android.connect.example.ui.settings.settingsComposables
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            ConnectSdkExampleTheme {
                NavHost(navController = navController, startDestination = MainDestination.ComponentPicker) {
                    composable(MainDestination.ComponentPicker) {
                        ComponentPickerContent(
                            viewModel = loaderViewModel,
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
