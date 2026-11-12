package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import kotlinx.coroutines.CoroutineScope

internal class PaymentElementAutocompleteAddressInteractor(
    override val autocompleteConfig: AutocompleteAddressInteractor.Config,
) : AutocompleteAddressInteractor {
    override fun register(onEvent: (AutocompleteAddressInteractor.Event) -> Unit) = Unit

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
                if (shouldUseAutocompleteProxyEndpointsProvider() && stripeAutocompleteRepository != null) {
                    activeInlineInteractor?.dispose()
                    return BillingInlineAutocompleteAddressInteractor(
                        placesClient = StripeHostedPlacesClientProxy(
                            repository = stripeAutocompleteRepository,
                            eventReporter = eventReporter,
                        ),
                        autocompleteConfig = AutocompleteAddressInteractor.Config(
                            autocompleteCountries = autocompleteConfig.autocompleteCountries,
                            isInlineAutocompleteEnabled = autocompleteConfig.isInlineAutocompleteEnabled,
                            shouldUseStripeHostedAutocomplete = true,
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
