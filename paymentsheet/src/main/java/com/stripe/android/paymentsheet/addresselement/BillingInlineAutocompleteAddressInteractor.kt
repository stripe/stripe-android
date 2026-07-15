package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.paymentsheet.injection.HostedPlacesClientProxyProvider
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

internal class BillingInlineAutocompleteAddressInteractor(
    placesClient: PlacesClientProxy,
    override val autocompleteConfig: AutocompleteAddressInteractor.Config,
    coroutineScope: CoroutineScope,
    shouldUseAutocompleteProxyEndpoints: Boolean,
) : AutocompleteAddressInteractor {
    private var eventListener: ((AutocompleteAddressInteractor.Event) -> Unit)? = null

    private val stripeHostedProxy: PlacesClientProxy? = if (shouldUseAutocompleteProxyEndpoints) {
        (placesClient as? HostedPlacesClientProxyProvider)?.createStripeHostedProxy()
    } else {
        null
    }

    private val inlineController = InlineAutocompleteController(
        placesClient = if (stripeHostedProxy != null) null else placesClient,
        stripeHostedProxy = stripeHostedProxy,
        config = autocompleteConfig,
        coroutineScope = coroutineScope,
        eventListenerProvider = { eventListener },
    )

    override val inlinePredictionsState: StateFlow<AutocompleteAddressInteractor.InlinePredictionsState> =
        inlineController.inlinePredictionsState

    override fun register(onEvent: (AutocompleteAddressInteractor.Event) -> Unit) {
        eventListener = onEvent
    }

    override fun onAutocomplete(country: String) = Unit

    override fun observeQueryChanges(query: StateFlow<String>, country: StateFlow<String?>) {
        inlineController.observeQueryChanges(query, country)
    }

    override fun onPredictionSelected(predictionId: String) {
        inlineController.onPredictionSelected(predictionId)
    }

    override fun onDismissed() {
        inlineController.onDismissed()
    }

    override fun onEnterManuallyFromInline() {
        eventListener?.invoke(AutocompleteAddressInteractor.Event.OnExpandForm(null))
    }

    fun dispose() {
        inlineController.dispose()
    }
}
