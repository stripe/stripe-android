package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

internal class PaymentElementAutocompleteAddressInteractor(
    private val launcher: AutocompleteLauncher,
    override val interactorScope: CoroutineScope,
    override val autocompleteConfig: AutocompleteAddressInteractor.Config,
) : AutocompleteAddressInteractor {
    override val autocompleteEvent = MutableSharedFlow<AutocompleteAddressInteractor.Event>()

    override fun onAutocomplete(country: String) {
        autocompleteConfig.googlePlacesApiKey?.let { googlePlacesApiKey ->
            launcher.launch(country, googlePlacesApiKey) { result ->
                val values = result.addressDetails?.toIdentifierMap()

                interactorScope.launch {
                    val event = when (result) {
                        is AutocompleteLauncher.Result.EnterManually -> {
                            AutocompleteAddressInteractor.Event.OnExpandForm(values)
                        }
                        is AutocompleteLauncher.Result.OnBack -> values?.let {
                            AutocompleteAddressInteractor.Event.OnValues(it)
                        }
                    }

                    event?.let {
                        autocompleteEvent.emit(it)
                    }
                }
            }
        }
    }

    class Factory(
        private val launcher: AutocompleteLauncher,
        private val interactorScope: CoroutineScope,
        private val autocompleteConfig: AutocompleteAddressInteractor.Config,
    ) : AutocompleteAddressInteractor.Factory {
        override fun create(): AutocompleteAddressInteractor {
            return PaymentElementAutocompleteAddressInteractor(
                launcher = launcher,
                interactorScope = interactorScope,
                autocompleteConfig = autocompleteConfig,
            )
        }
    }
}
