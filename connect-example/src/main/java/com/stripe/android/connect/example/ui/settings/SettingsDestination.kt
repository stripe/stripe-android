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
    const val PresentationSettings = "PresentationSettings"
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
            openOnboardingSettings = { navController.navigate(SettingsDestination.OnboardingSettings) },
            openPresentationSettings = { navController.navigate(SettingsDestination.PresentationSettings) }
        )
    }
    composable(SettingsDestination.OnboardingSettings) {
        val settingsViewModel = hiltViewModel<SettingsViewModel>(activity)
        AccountOnboardingSettingsView(
            viewModel = settingsViewModel,
            onBack = { navController.safeNavigateUp() },
        )
    }
    composable(SettingsDestination.PresentationSettings) {
        val settingsViewModel = hiltViewModel<SettingsViewModel>(activity)
        PresentationSettingsView(
            viewModel = settingsViewModel,
            onBack = { navController.safeNavigateUp() },
        )
    }
}
