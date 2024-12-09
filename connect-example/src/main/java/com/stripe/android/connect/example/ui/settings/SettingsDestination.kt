package com.stripe.android.connect.example.ui.settings

import androidx.activity.ComponentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.stripe.android.connect.example.core.safeNavigateUp
import com.stripe.android.connect.example.ui.embeddedcomponentmanagerloader.EmbeddedComponentLoaderViewModel

@Suppress("ConstPropertyName")
object SettingsDestination {
    const val Settings = "Settings"
    const val OnboardingSettings = "OnboardingSettings"
}

fun NavGraphBuilder.settingsComposables(
    activity: ComponentActivity,
    navController: NavHostController,
) {
    composable(SettingsDestination.Settings) {
        val loaderViewmodel = hiltViewModel<EmbeddedComponentLoaderViewModel>(activity)
        val settingsViewModel = hiltViewModel<SettingsViewModel>(activity)
        SettingsView(
            viewModel = settingsViewModel,
            onDismiss = { navController.safeNavigateUp() },
            onReloadRequested = loaderViewmodel::reload,
            openOnboardingSettings = { navController.navigate(SettingsDestination.OnboardingSettings) }
        )
    }
    composable(SettingsDestination.OnboardingSettings) {
        AccountOnboardingSettingsView(
            onBack = { navController.safeNavigateUp() },
        )
    }
}
