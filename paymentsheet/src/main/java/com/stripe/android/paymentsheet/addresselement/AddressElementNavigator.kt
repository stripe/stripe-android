package com.stripe.android.paymentsheet.addresselement

import android.os.Parcelable
import androidx.navigation.NavHostController
import com.stripe.android.paymentsheet.PaymentSheet
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

    fun dismiss(result: AddressLauncherResult = AddressLauncherResult.Canceled())

    fun requestDismiss()

    fun setDismissRequestHandler(handler: (() -> Unit)?)

    fun canDismiss(): Boolean

    fun setDismissGuard(guard: (() -> Boolean)?)

    fun onBack()

    sealed interface AutocompleteEvent : Parcelable {
        val address: PaymentSheet.Address?

        @Parcelize
        data class OnBack(override val address: PaymentSheet.Address?) : AutocompleteEvent

        @Parcelize
        data class OnEnterManually(override val address: PaymentSheet.Address?) : AutocompleteEvent

        companion object {
            const val KEY = "AutocompleteEvent"
        }
    }
}

@Singleton
internal class NavHostAddressElementNavigator @Inject constructor() : AddressElementNavigator {
    var navigationController: NavHostController? = null
    var onDismiss: ((AddressLauncherResult) -> Unit)? = null
    var onDismissRequest: (() -> Unit)? = null
    var onDismissGuard: (() -> Boolean)? = null

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

    override fun requestDismiss() {
        onDismissRequest?.invoke() ?: dismiss()
    }

    override fun setDismissRequestHandler(handler: (() -> Unit)?) {
        onDismissRequest = handler
    }

    override fun canDismiss(): Boolean = onDismissGuard?.invoke() ?: true

    override fun setDismissGuard(guard: (() -> Boolean)?) {
        onDismissGuard = guard
    }

    override fun onBack() {
        navigationController?.let { navController ->
            if (!navController.popBackStack()) {
                requestDismiss()
            }
        }
    }
}
