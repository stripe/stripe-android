package com.stripe.android.paymentsheet.addresselement

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.paymentsheet.injection.AutocompleteViewModelSubcomponent
import com.stripe.android.paymentsheet.injection.DaggerAddressElementViewModelFactoryComponent
import com.stripe.android.paymentsheet.injection.InputAddressViewModelSubcomponent
import javax.inject.Inject
import javax.inject.Provider

internal class AddressElementViewModel @Inject internal constructor(
    val navigator: NavHostAddressElementNavigator,
    val inputAddressViewModelSubcomponentFactoryProvider: Provider<InputAddressViewModelSubcomponent.Factory>,
    val autoCompleteViewModelSubcomponentFactoryProvider: Provider<AutocompleteViewModelSubcomponent.Factory>,
) : ViewModel() {

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
