package com.stripe.android.connect.example.core

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController

/**
 * Safer [NavHostController.navigateUp] that prevents navigating past the previous screen,
 * typically due to accidental double-clicks.
 */
fun NavHostController.safeNavigateUp() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        navigateUp()
    }
}
