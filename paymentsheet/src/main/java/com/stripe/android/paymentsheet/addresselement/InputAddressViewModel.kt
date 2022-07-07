package com.stripe.android.paymentsheet.addresselement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.ui.core.injection.NonFallbackInjectable
import com.stripe.android.paymentsheet.injection.InputAddressViewModelSubcomponent
import com.stripe.android.ui.core.injection.FormControllerSubcomponent
import com.stripe.android.ui.core.injection.NonFallbackInjector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

internal class InputAddressViewModel @Inject constructor(
    val args: AddressElementActivityContract.Args,
    val navigator: AddressElementNavigator,
    private val formControllerProvider: Provider<FormControllerSubcomponent.Builder>
) : ViewModel() {

    private val _collectedAddress = MutableStateFlow<ShippingAddress?>(null)
    val collectedAddress: StateFlow<ShippingAddress?> = _collectedAddress

    init {
        viewModelScope.launch {
            navigator.getResultFlow<ShippingAddress?>(ShippingAddress.KEY)?.collect {
                val oldShippingAddress = _collectedAddress.value
                _collectedAddress.emit(
                    ShippingAddress(
                        name = oldShippingAddress?.name ?: it?.name,
                        company = oldShippingAddress?.company ?: it?.company,
                        phoneNumber = oldShippingAddress?.phoneNumber ?: it?.phoneNumber,
                        city = it?.city,
                        country = it?.country,
                        line1 = it?.line1,
                        line2 = it?.line2,
                        state = it?.state,
                        postalCode = it?.postalCode
                    )
                )
            }
        }
    }

    internal class Factory(
        private val injector: NonFallbackInjector
    ) : ViewModelProvider.Factory, NonFallbackInjectable {

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<InputAddressViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            injector.inject(this)
            return subComponentBuilderProvider.get()
                .build().inputAddressViewModel as T
        }
    }
}
