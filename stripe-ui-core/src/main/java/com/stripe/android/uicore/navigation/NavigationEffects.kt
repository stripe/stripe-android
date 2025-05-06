package com.stripe.android.uicore.navigation

import android.app.Activity
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
    onPopBackStackResult: (Boolean) -> Unit = {},
    onBackStackEntryUpdated: (NavBackStackEntryUpdate) -> Unit,
) {
    val activity = (LocalContext.current as? Activity)
    val backStackEntry by navHostController.currentBackStackEntryAsState()

    LaunchedEffect(backStackEntry) {
        val update = NavBackStackEntryUpdate(
            previousBackStackEntry = navHostController.previousBackStackEntry,
            currentBackStackEntry = backStackEntry,
        )
        onBackStackEntryUpdated(update)
    }

    LaunchedEffect(activity, navHostController, navigationChannel) {
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
                                applyPop(
                                    navHostController = navHostController,
                                    currentRoute = from,
                                    popUpTo = intent.popUpTo
                                )
                            }
                        }
                    }
                }

                NavigationIntent.NavigateBack -> {
                    val handled = navHostController.popBackStack()
                    onPopBackStackResult(handled)
                }
            }
        }.launchIn(this)
    }
}

private fun NavOptionsBuilder.applyPop(
    navHostController: NavHostController,
    currentRoute: String?,
    popUpTo: PopUpToBehavior,
) {
    when (popUpTo) {
        is PopUpToBehavior.Current -> currentRoute?.let {
            popUpTo(it) {
                inclusive = popUpTo.inclusive
            }
        }
        is PopUpToBehavior.Route -> popUpTo(popUpTo.route) {
            inclusive = popUpTo.inclusive
        }
        PopUpToBehavior.Start -> popUpTo(
            navHostController.graph.id
        ) {
            inclusive = popUpTo.inclusive
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class NavBackStackEntryUpdate(
    val previousBackStackEntry: NavBackStackEntry?,
    val currentBackStackEntry: NavBackStackEntry?
)
