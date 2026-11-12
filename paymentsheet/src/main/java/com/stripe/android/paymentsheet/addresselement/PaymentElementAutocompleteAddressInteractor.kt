package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import kotlinx.coroutines.CoroutineScope

internal class PaymentElementAutocompleteAddressInteractor(
    override val autocompleteConfig: AutocompleteAddressInteractor.Config,
) : AutocompleteAddressInteractor {
    override fun register(onEvent: (AutocompleteAddressInteractor.Event) -> Unit) = Unit

    class Factory(
        private val autocompleteConfig: AutocompleteAddressInteractor.Config,
        private val placesClient: PlacesClientProxy?,
        private val stripeAutocompleteRepository: StripeAutocompleteRepository?,
        private val coroutineScope: CoroutineScope?,
        private val shouldUseAutocompleteProxyEndpointsProvider: () -> Boolean,
        private val eventReporter: AddressLauncherEventReporter,
    ) : AutocompleteAddressInteractor.Factory {
        private var activeInlineInteractor: BillingInlineAutocompleteAddressInteractor? = null

        val autocompleteFilledAddress: com.stripe.android.model.Address?
            get() = activeInlineInteractor?.autocompleteFilledAddress

        override fun create(): AutocompleteAddressInteractor {
            if (coroutineScope != null && autocompleteConfig.isInlineAutocompleteEnabled) {
                val useStripeHosted = shouldUseAutocompleteProxyEndpointsProvider()
                val resolvedClient = when {
                    useStripeHosted && stripeAutocompleteRepository != null ->
                        StripeHostedPlacesClientProxy(
                            repository = stripeAutocompleteRepository,
                            eventReporter = eventReporter,
                        )
                    useStripeHosted -> null
                    else -> placesClient
                }
                if (resolvedClient != null) {
                    activeInlineInteractor?.dispose()
                    return BillingInlineAutocompleteAddressInteractor(
                        placesClient = resolvedClient,
                        autocompleteConfig = AutocompleteAddressInteractor.Config(
                            googlePlacesApiKey = autocompleteConfig.googlePlacesApiKey,
                            autocompleteCountries = autocompleteConfig.autocompleteCountries,
                            isInlineAutocompleteEnabled = autocompleteConfig.isInlineAutocompleteEnabled,
                            shouldUseStripeHostedAutocomplete = useStripeHosted ||
                                autocompleteConfig.shouldUseStripeHostedAutocomplete,
                        ),
                        coroutineScope = coroutineScope,
                    ).also { activeInlineInteractor = it }
                }
            }

            activeInlineInteractor?.dispose()
            activeInlineInteractor = null
            return PaymentElementAutocompleteAddressInteractor(
                autocompleteConfig = autocompleteConfig,
            )
        }
    }
}
