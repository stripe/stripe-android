package com.stripe.android.link.utils

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import com.stripe.android.link.LinkScreen

internal val enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
    if (initialState.destination.route == LinkScreen.Loading.route) {
        // If we're just showing the loading screen, fade in for a calmer experience
        fadeIn()
    } else {
        // Slide in from the right
        fadeIn() + slideInHorizontally { it }
    }
}

internal val popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
    // Slide out to the left
    fadeIn() + slideInHorizontally { -it }
}

internal val popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
    // Leave to the right
    fadeOut() + slideOutHorizontally { it }
}

internal val exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
    // Slide out to the left
    fadeOut() + slideOutHorizontally { -it }
}
