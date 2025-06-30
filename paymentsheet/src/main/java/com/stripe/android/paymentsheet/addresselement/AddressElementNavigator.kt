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
internal interface AddressElementNavigator {
    fun navigateTo(
        target: AddressElementScreen
    )

    fun setResult(key: String, value: Any?)

    fun <T : Any?> getResultFlow(key: String): Flow<T>?

    fun dismiss(result: AddressLauncherResult = AddressLauncherResult.Canceled)

    fun onBack()

    companion object {
        internal const val FORCE_EXPANDED_FORM_KEY = "force_expanded_form"
    }
}

@Singleton
internal class NavHostAddressElementNavigator @Inject constructor() : AddressElementNavigator {
    var navigationController: NavHostController? = null
    var onDismiss: ((AddressLauncherResult) -> Unit)? = null

    override fun navigateTo(
        target: AddressElementScreen
    ) {
        navigationController?.navigate(target.route)
    }

    override fun setResult(key: String, value: Any?) {
        navigationController?.previousBackStackEntry?.savedStateHandle?.set(key, value)
    }

    override fun <T : Any?> getResultFlow(key: String): Flow<T>? {
        val currentBackStackEntry = navigationController?.currentBackStackEntry ?: return null
        return currentBackStackEntry
            .savedStateHandle
            .getStateFlow<T?>(key, initialValue = null)
            .filterNotNull()
    }

    override fun dismiss(result: AddressLauncherResult) {
        onDismiss?.invoke(result)
    }

    override fun onBack() {
        navigationController?.let { navController ->
            if (!navController.popBackStack()) {
                dismiss()
            }
        }
    }
}
