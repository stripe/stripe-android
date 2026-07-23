package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import kotlinx.coroutines.CoroutineScope

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
        val values = result.address?.toIdentifierMap()

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
        private val placesClientProvider: () -> PlacesClientProxy?,
        private val coroutineScope: CoroutineScope?,
    ) : AutocompleteAddressInteractor.Factory {
        private var activeInlineInteractor: BillingInlineAutocompleteAddressInteractor? = null

        override fun create(): AutocompleteAddressInteractor {
            val effectivePlacesClient = placesClientProvider()
            val canCreateInline = effectivePlacesClient != null &&
                coroutineScope != null &&
                autocompleteConfig.isInlineAutocompleteEnabled
            if (canCreateInline) {
                activeInlineInteractor?.dispose()
                return BillingInlineAutocompleteAddressInteractor(
                    placesClient = effectivePlacesClient,
                    autocompleteConfig = AutocompleteAddressInteractor.Config(
                        googlePlacesApiKey = autocompleteConfig.googlePlacesApiKey,
                        autocompleteCountries = autocompleteConfig.autocompleteCountries,
                        isPlacesAvailable = autocompleteConfig.isPlacesAvailable,
                        isInlineAutocompleteEnabled = autocompleteConfig.isInlineAutocompleteEnabled,
                        shouldUseStripeHostedAutocomplete =
                            effectivePlacesClient is StripeHostedPlacesClientProxy ||
                            autocompleteConfig.shouldUseStripeHostedAutocomplete,
                    ),
                    coroutineScope = coroutineScope!!,
                ).also { activeInlineInteractor = it }
            }
            return PaymentElementAutocompleteAddressInteractor(
                launcher = launcher,
                autocompleteConfig = autocompleteConfig,
            )
        }
    }
}
