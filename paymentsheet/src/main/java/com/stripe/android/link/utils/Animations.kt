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
    // Slide in from the right
    fadeIn() + slideInHorizontally { it }
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

internal val loadingExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    if (targetState.destination.route == LinkScreen.SignUp.route) {
        fadeOut()
    } else {
        exitTransition()
    }
}

internal val signupEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    if (initialState.destination.route == LinkScreen.Loading.route) {
        fadeIn()
    } else {
        enterTransition()
    }
}
