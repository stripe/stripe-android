package com.stripe.android.paymentsheet.addresselement

import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates the navigation between screens.
 * TODO combine this with Link's navigator class.
 */
@Singleton
internal class AddressElementNavigator @Inject constructor() {
    var navigationController: NavHostController? = null
    var onDismiss: ((AddressLauncherResult) -> Unit)? = null

    fun navigateTo(
        target: AddressElementScreen
    ) = navigationController?.navigate(target.route)

    fun setResult(key: String, value: Any?) =
        navigationController?.previousBackStackEntry?.savedStateHandle?.set(key, value)

    fun <T : Any?> getResultFlow(key: String): Flow<T>? {
        val currentBackStackEntry = navigationController?.currentBackStackEntry ?: return null
        return currentBackStackEntry
            .savedStateHandle
            .getStateFlow<T?>(key, initialValue = null)
            .filterNotNull()
    }

    fun dismiss(result: AddressLauncherResult = AddressLauncherResult.Canceled) {
        onDismiss?.invoke(result)
    }

    fun onBack() {
        navigationController?.let { navController ->
            if (!navController.popBackStack()) {
                dismiss()
            }
        }
    }
}
