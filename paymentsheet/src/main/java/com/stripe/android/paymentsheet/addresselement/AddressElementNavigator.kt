package com.stripe.android.paymentsheet.addresselement

import androidx.lifecycle.asFlow
import androidx.navigation.NavHostController
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates the navigation between screens.
 * TODO combine this with Link's navigator class.
 */
@Singleton
internal class AddressElementNavigator @Inject constructor() {
    var navigationController: NavHostController? = null
    var onDismiss: ((AddressElementResult) -> Unit)? = null

    fun navigateTo(
        target: AddressElementScreen
    ) = navigationController?.navigate(target.route)

    fun setResult(key: String, value: Any) =
        navigationController?.previousBackStackEntry?.savedStateHandle?.set(key, value)

    fun <T> getResultFlow(key: String) =
        navigationController?.currentBackStackEntry?.savedStateHandle?.getLiveData<T>(key)?.asFlow()

    fun dismiss(result: AddressElementResult = AddressElementResult.Canceled) =
        onDismiss?.let {
            it(result)
        }

    fun onBack() =
        navigationController?.let { navController ->
            if (!navController.popBackStack()) {
                dismiss()
            }
        }
}
