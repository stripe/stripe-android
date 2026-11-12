package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import kotlinx.coroutines.CoroutineScope

// Stripe-hosted autocomplete is unavailable (shouldUseAutocompleteProxyEndpointsProvider is false
// or there's no active coroutine scope to run it in); real Google Places autocomplete is no
// longer supported, so autocomplete is disabled entirely rather than falling back to it.
internal class PaymentElementAutocompleteAddressInteractor(
    override val autocompleteConfig: AutocompleteAddressInteractor.Config,
) : AutocompleteAddressInteractor {
    private var eventListener: ((AutocompleteAddressInteractor.Event) -> Unit)? = null

    override fun register(onEvent: (AutocompleteAddressInteractor.Event) -> Unit) {
        eventListener = onEvent
    }

    override fun onAutocomplete(country: String) = Unit

    class Factory(
        private val autocompleteConfig: AutocompleteAddressInteractor.Config,
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
                val resolvedClient = if (useStripeHosted && stripeAutocompleteRepository != null) {
                    StripeHostedPlacesClientProxy(
                        repository = stripeAutocompleteRepository,
                        eventReporter = eventReporter,
                    )
                } else {
                    null
                }
                if (resolvedClient != null) {
                    activeInlineInteractor?.dispose()
                    return BillingInlineAutocompleteAddressInteractor(
                        placesClient = resolvedClient,
                        autocompleteConfig = AutocompleteAddressInteractor.Config(
                            googlePlacesApiKey = autocompleteConfig.googlePlacesApiKey,
                            autocompleteCountries = autocompleteConfig.autocompleteCountries,
                            isPlacesAvailable = autocompleteConfig.isPlacesAvailable,
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
