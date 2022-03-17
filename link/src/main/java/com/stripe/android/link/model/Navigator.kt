package com.stripe.android.link.model

import androidx.navigation.NavHostController
import com.stripe.android.link.LinkScreen
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates the navigation between screens.
 */
@Singleton
internal class Navigator @Inject constructor() {
    var navigationController: NavHostController? = null
    var onDismiss = {}

    /**
     * Navigates to the given [LinkScreen], optionally clearing the back stack.
     */
    fun navigateTo(
        target: LinkScreen,
        clearBackStack: Boolean = false
    ) = navigationController?.let { navController ->
        navController.navigate(target.route) {
            if (clearBackStack) {
                popUpTo(navController.backQueue.first().destination.id)
            }
        }
    }

    /**
     * Behaves like a back button, popping the back stack and dismissing the Activity if this was
     * the last screen.
     */
    fun onBack() =
        navigationController?.let { navController ->
            if (!navController.popBackStack()) {
                onDismiss()
            }
        }

    fun dismiss() {
        onDismiss()
    }
}
