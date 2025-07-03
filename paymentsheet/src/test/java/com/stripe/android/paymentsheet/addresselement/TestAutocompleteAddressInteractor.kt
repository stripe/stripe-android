package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow

class TestAutocompleteAddressInteractor(
    override val interactorScope: CoroutineScope,
    config: AutocompleteAddressInteractor.Config = AutocompleteAddressInteractor.Config(
        googlePlacesApiKey = null,
        autocompleteCountries = emptySet()
    ),
    override val autocompleteEvent: MutableSharedFlow<AutocompleteAddressInteractor.Event> = MutableSharedFlow(),
) : AutocompleteAddressInteractor {
    override val autocompleteConfig: AutocompleteAddressInteractor.Config = config

    override fun onAutocomplete(country: String) {
        error("Should not be called!")
    }
}
