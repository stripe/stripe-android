package com.stripe.android.paymentsheet.addresselement

import android.os.Parcelable
import androidx.navigation.NavHostController
import com.stripe.android.elements.AddressDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.parcelize.Parcelize
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

    fun dismiss(result: AddressLauncher.Result = AddressLauncher.Result.Canceled)

    fun onBack()

    sealed interface AutocompleteEvent : Parcelable {
        val addressDetails: AddressDetails?

        @Parcelize
        data class OnBack(override val addressDetails: AddressDetails?) : AutocompleteEvent

        @Parcelize
        data class OnEnterManually(override val addressDetails: AddressDetails?) : AutocompleteEvent

        companion object {
            const val KEY = "AutocompleteEvent"
        }
    }
}

@Singleton
internal class NavHostAddressElementNavigator @Inject constructor() : AddressElementNavigator {
    var navigationController: NavHostController? = null
    var onDismiss: ((AddressLauncher.Result) -> Unit)? = null

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

    override fun dismiss(result: AddressLauncher.Result) {
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
