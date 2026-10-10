package com.stripe.android.paymentsheet.addresselement

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.paymentsheet.addresselement.AddressElementResultStateHolder.State
import com.stripe.android.paymentsheet.injection.AutocompleteViewModelSubcomponent
import com.stripe.android.paymentsheet.injection.DaggerAddressElementViewModelFactoryComponent
import com.stripe.android.paymentsheet.injection.InputAddressViewModelSubcomponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Provider

internal class AddressElementViewModel @Inject internal constructor(
    val navigator: NavHostAddressElementNavigator,
    val resultStateHolder: AddressElementResultStateHolder,
    val inputAddressViewModelSubcomponentFactoryProvider: Provider<InputAddressViewModelSubcomponent.Factory>,
    val autoCompleteViewModelSubcomponentFactoryProvider: Provider<AutocompleteViewModelSubcomponent.Factory>,
) : ViewModel() {

    private val _showDiscardConfirmation = MutableStateFlow(false)
    val showDiscardConfirmation: StateFlow<Boolean> = _showDiscardConfirmation.asStateFlow()

    fun dismiss(shouldConfirmDismissal: Boolean) {
        if (canDismiss(shouldConfirmDismissal)) {
            resultStateHolder.onUserCancel()
        }
    }

    fun canDismiss(shouldConfirmDismissal: Boolean): Boolean {
        return when (resultStateHolder.state.value) {
            State.Idle -> {
                if (shouldConfirmDismissal) {
                    _showDiscardConfirmation.value = true
                    false
                } else {
                    true
                }
            }
            State.Saving -> false
            is State.Finished -> true
        }
    }

    fun onBack(shouldConfirmDismissal: Boolean) {
        if (resultStateHolder.state.value != State.Idle) return

        if (!navigator.onBack()) {
            dismiss(shouldConfirmDismissal)
        }
    }

    fun keepEditing() {
        _showDiscardConfirmation.value = false
    }

    fun discardChanges() {
        keepEditing()
        resultStateHolder.onUserCancel()
    }

    internal class Factory(
        private val applicationSupplier: () -> Application,
        private val starterArgsSupplier: () -> AddressElementActivityContract.Args
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DaggerAddressElementViewModelFactoryComponent.factory()
                .create(
                    context = applicationSupplier(),
                    starterArgs = starterArgsSupplier(),
                )
                .addressElementViewModel as T
        }
    }
}
