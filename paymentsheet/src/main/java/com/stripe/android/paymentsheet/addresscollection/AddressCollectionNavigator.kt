package com.stripe.android.paymentsheet.addresscollection

import androidx.lifecycle.asFlow
import androidx.navigation.NavHostController
import com.stripe.android.link.LinkActivityResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates the navigation between screens.
 */
@Singleton
internal class AddressCollectionNavigator @Inject constructor() {
    var navigationController: NavHostController? = null

    fun navigateTo(
        target: AddressCollectionScreen
    ) = navigationController?.let { navController ->
        navController.navigate(target.route) {
            popUpTo(navController.backQueue.first().destination.id) {
                inclusive = true
            }
        }
    }

    fun setResult(key: String, value: Any) =
        navigationController?.previousBackStackEntry?.savedStateHandle?.set(key, value)

    fun <T> getResultFlow(key: String) =
        navigationController?.currentBackStackEntry?.savedStateHandle?.getLiveData<T>(key)?.asFlow()
}