package com.stripe.android.paymentsheet.addresselement

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.paymentsheet.injection.AutocompleteViewModelSubcomponent
import com.stripe.android.paymentsheet.injection.DaggerAddressElementViewModelFactoryComponent
import com.stripe.android.paymentsheet.injection.InputAddressViewModelSubcomponent
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Provider

internal class AddressElementViewModel @Inject internal constructor(
    val navigator: NavHostAddressElementNavigator,
    val resultStateHolder: AddressElementResultStateHolder,
    val inputAddressViewModelSubcomponentFactoryProvider: Provider<InputAddressViewModelSubcomponent.Factory>,
    val autoCompleteViewModelSubcomponentFactoryProvider: Provider<AutocompleteViewModelSubcomponent.Factory>,
    private val dismissalCoordinator: AddressElementDismissalCoordinator,
) : ViewModel() {

    val showDiscardConfirmation: StateFlow<Boolean> =
        dismissalCoordinator.showDiscardConfirmation

    fun dismiss() {
        if (canDismiss()) {
            resultStateHolder.setResult(AddressElementActivityContract.Result.Canceled)
        }
    }

    fun canDismiss(): Boolean {
        return resultStateHolder.result.value != null ||
            !FeatureFlags.enableAddressElementUnsavedChanges.isEnabled ||
            (!dismissalCoordinator.isSaving && dismissalCoordinator.requestDismiss())
    }

    fun onBack() {
        if (!navigator.onBack()) {
            dismiss()
        }
    }

    fun keepEditing() {
        dismissalCoordinator.keepEditing()
    }

    fun discardChanges() {
        dismissalCoordinator.discardChanges()
        resultStateHolder.setResult(AddressElementActivityContract.Result.Canceled)
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
