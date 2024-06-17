package com.stripe.android.financialconnections.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane.ATTACH_LINKED_PAYMENT_ACCOUNT
import com.stripe.android.financialconnections.navigation.pane

private const val TransitionDurationMillis = 300

private val FADE_IN_TRANSITION = fadeIn(
    animationSpec = tween(TransitionDurationMillis)
)

private val FADE_OUT_TRANSITION = fadeOut(
    animationSpec = tween(TransitionDurationMillis)
)

internal fun enterTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition {
    return {
        if (initialState.skipTransition) {
            FADE_IN_TRANSITION
        } else {
            FADE_IN_TRANSITION + slideInHorizontally(
                animationSpec = tween(TransitionDurationMillis),
                initialOffsetX = { fullWidth ->
                    fullWidth / 2
                },
            )
        }
    }
}

internal fun resumeTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition {
    return {
        if (initialState.skipTransition) {
            FADE_IN_TRANSITION
        } else {
            FADE_IN_TRANSITION + slideInHorizontally(
                animationSpec = tween(TransitionDurationMillis),
                initialOffsetX = { fullWidth ->
                    -fullWidth / 2
                },
            )
        }
    }
}

internal fun pauseTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition {
    return {
        if (initialState.skipTransition) {
            FADE_OUT_TRANSITION
        } else {
            FADE_OUT_TRANSITION + slideOutHorizontally(
                animationSpec = tween(TransitionDurationMillis),
                targetOffsetX = { fullWidth ->
                    -fullWidth / 2
                },
            )
        }
    }
}

internal fun exitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition {
    return {
        if (initialState.skipTransition) {
            FADE_OUT_TRANSITION
        } else {
            FADE_OUT_TRANSITION + slideOutHorizontally(
                animationSpec = tween(TransitionDurationMillis),
                targetOffsetX = { fullWidth ->
                    fullWidth / 2
                },
            )
        }
    }
}

/**
 * We want to skip the transition for some screens and use a simple fade instead. This currently only applies
 * to transitions starting from the attach linked payment account screen, which only ever shows a loading indicator.
 * We wouldn't want to show a slide transition from one loading indicator to another, therefore we use a fade.
 */
private val NavBackStackEntry.skipTransition: Boolean
    get() = destination.pane == ATTACH_LINKED_PAYMENT_ACCOUNT
