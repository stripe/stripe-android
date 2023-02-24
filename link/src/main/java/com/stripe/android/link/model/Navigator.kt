package com.stripe.android.link.model

import androidx.lifecycle.asFlow
import androidx.navigation.NavHostController
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkActivityResult.Canceled.Reason.BackPressed
import com.stripe.android.link.LinkScreen
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates the navigation between screens.
 */
@Singleton
internal class Navigator @Inject constructor() {
    var userNavigationEnabled = true
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
                popUpTo(navController.backQueue.first().destination.id) {
                    inclusive = true
                }
            }
        }
    }

    fun setResult(key: String, value: Any) =
        navigationController?.previousBackStackEntry?.savedStateHandle?.set(key, value)

    fun <T> getResultFlow(key: String) =
        navigationController?.currentBackStackEntry?.savedStateHandle?.getLiveData<T>(key)?.asFlow()

    /**
     * Behaves like a back button, popping the back stack and dismissing the Activity if this was
     * the last screen.
     * When [userInitiated] is true, only performs any action if [userNavigationEnabled] is true.
     *
     * @param userInitiated Whether the action was initiated by user interaction.
     */
    fun onBack(userInitiated: Boolean) {
        if (!userInitiated || userNavigationEnabled) {
            navigationController?.let { navController ->
                if (!navController.popBackStack()) {
                    cancel(reason = BackPressed)
                }
            }
        }
    }

    /**
     * Dismisses the Link Activity with the given [result].
     */
    fun dismiss(result: LinkActivityResult) = onDismiss?.invoke(result)

    /**
     * Cancels the Link Activity with the given [reason].
     */
    fun cancel(reason: LinkActivityResult.Canceled.Reason) {
        dismiss(LinkActivityResult.Canceled(reason))
    }

    fun isOnRootScreen() = navigationController?.isOnRootScreen()

    fun unregister() {
        onDismiss = null
        navigationController = null
    }
}

// The Loading screen is always at the bottom of the stack, so a size of 2 means the current
// screen is at the bottom of the navigation stack.
fun NavHostController.isOnRootScreen() = backQueue.size <= 2
