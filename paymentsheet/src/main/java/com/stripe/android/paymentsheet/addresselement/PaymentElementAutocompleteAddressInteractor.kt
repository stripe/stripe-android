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
        private val inlineDependencies: InlineAutocompleteDependencies?,
    ) : AutocompleteAddressInteractor.Factory {
        // The factory outlives individual forms (it is held for the whole sheet), so it disposes the
        // previously-created inline interactor before building a new one. Only one address form is
        // live at a time in this flow, so the prior controller's long-running query collector would
        // otherwise leak on the shared coroutine scope across form re-creations.
        private var activeInlineInteractor: BillingInlineAutocompleteAddressInteractor? = null

        override fun create(): AutocompleteAddressInteractor {
            activeInlineInteractor?.dispose()
            activeInlineInteractor = null

            val dependencies = inlineDependencies
            if (dependencies != null) {
                return BillingInlineAutocompleteAddressInteractor(
                    placesClient = dependencies.placesClient,
                    autocompleteConfig = autocompleteConfig,
                    coroutineScope = dependencies.coroutineScope,
                ).also { activeInlineInteractor = it }
            }
            return PaymentElementAutocompleteAddressInteractor(
                launcher = launcher,
                autocompleteConfig = autocompleteConfig,
            )
        }
    }
}

internal data class InlineAutocompleteDependencies(
    val placesClient: PlacesClientProxy,
    val coroutineScope: CoroutineScope,
)
