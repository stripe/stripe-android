package com.stripe.android.link.model

import androidx.navigation.NavHostController
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkScreen
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates the navigation between screens.
 */
@Singleton
internal class Navigator @Inject constructor() {
    var navigationController: NavHostController? = null
    var onDismiss: ((LinkActivityResult) -> Unit)? = null

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
                dismiss()
            }
        }

    /**
     * Dismisses the Link Activity with the given [result].
     */
    fun dismiss(result: LinkActivityResult = LinkActivityResult.Canceled) =
        onDismiss?.let {
            it(result)
        }
}
