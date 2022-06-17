package com.stripe.android.paymentsheet.addresselement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.ui.core.injection.NonFallbackInjectable
import com.stripe.android.paymentsheet.injection.AutoCompleteViewModelSubcomponent
import com.stripe.android.ui.core.injection.NonFallbackInjector
import javax.inject.Inject
import javax.inject.Provider

internal class AutoCompleteViewModel @Inject constructor(
    val args: AddressElementActivityContract.Args,
    val navigator: AddressElementNavigator
) : ViewModel() {

    internal class Factory(
        private val injector: NonFallbackInjector
    ) : ViewModelProvider.Factory, NonFallbackInjectable {

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<AutoCompleteViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            injector.inject(this)
            return subComponentBuilderProvider.get()
                .build().autoCompleteViewModel as T
        }
    }
}