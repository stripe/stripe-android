package com.stripe.android.uicore.navigation

import android.app.Activity
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun NavigationEffects(
    navigationChannel: SharedFlow<NavigationIntent>,
    navHostController: NavHostController,
    keyboardController: KeyboardController,
    onBackStackEntryUpdated: (NavBackStackEntry?) -> Unit
) {
    val activity = (LocalContext.current as? Activity)
    val backStackEntry by navHostController.currentBackStackEntryAsState()

    LaunchedEffect(activity, navHostController, navigationChannel) {
        Log.d("NavigationEffects", "LaunchedEffect registered")
        navigationChannel.onEach { intent ->
            if (activity?.isFinishing == true) {
                return@onEach
            }

            keyboardController.dismiss()

            when (intent) {
                is NavigationIntent.NavigateTo -> {
                    val from: String? = navHostController.currentDestination?.route
                    val destination: String = intent.route

                    if (destination.isNotEmpty() && destination != from) {
                        navHostController.navigate(destination) {
                            launchSingleTop = intent.isSingleTop

                            if (intent.popUpTo != null) {
                                apply(from, intent.popUpTo)
                            }
                        }
                    }
                }

                NavigationIntent.NavigateBack -> {
                    navHostController.popBackStack()
                }
            }
        }.launchIn(this)
    }

    LaunchedEffect(backStackEntry) {
        onBackStackEntryUpdated(backStackEntry)
    }
}

private fun NavOptionsBuilder.apply(
    currentRoute: String?,
    popUpTo: PopUpToBehavior,
) {
    val popUpToRoute = when (popUpTo) {
        is PopUpToBehavior.Current -> currentRoute
        is PopUpToBehavior.Route -> popUpTo.route
    }

    if (popUpToRoute != null) {
        popUpTo(popUpToRoute) {
            inclusive = popUpTo.inclusive
        }
    }
}
