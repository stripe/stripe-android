package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.elements.toIdentifierMap
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor

internal class PaymentElementAutocompleteAddressInteractor(
    private val launcher: AutocompleteLauncher,
    override val autocompleteConfig: AutocompleteAddressInteractor.Config,
) : AutocompleteAddressInteractor, AutocompleteLauncherResultHandler {
    private var eventListener: ((AutocompleteAddressInteractor.Event) -> Unit)? = null

    override fun register(onEvent: (AutocompleteAddressInteractor.Event) -> Unit) {
        eventListener = onEvent
    }

    override fun onAutocomplete(country: String) {
        autocompleteConfig.googlePlacesApiKey?.let { googlePlacesApiKey ->
            launcher.launch(
                country = country,
                googlePlacesApiKey = googlePlacesApiKey,
                resultHandler = this,
            )
        }
    }

    override fun onAutocompleteLauncherResult(result: AutocompleteLauncher.Result) {
        val values = result.addressDetails?.toIdentifierMap()

        val event = when (result) {
            is AutocompleteLauncher.Result.EnterManually -> {
                AutocompleteAddressInteractor.Event.OnExpandForm(values)
            }
            is AutocompleteLauncher.Result.OnBack -> values?.let {
                AutocompleteAddressInteractor.Event.OnValues(it)
            }
        }

        event?.let {
            eventListener?.invoke(it)
        }
    }

    class Factory(
        private val launcher: AutocompleteLauncher,
        private val autocompleteConfig: AutocompleteAddressInteractor.Config,
    ) : AutocompleteAddressInteractor.Factory {
        override fun create(): AutocompleteAddressInteractor {
            return PaymentElementAutocompleteAddressInteractor(
                launcher = launcher,
                autocompleteConfig = autocompleteConfig,
            )
        }
    }
}
