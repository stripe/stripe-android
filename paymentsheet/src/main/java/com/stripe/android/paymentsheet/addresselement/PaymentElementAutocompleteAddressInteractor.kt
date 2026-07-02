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
}

internal class PaymentElementAutocompleteAddressInteractorFactory(
    private val launcher: AutocompleteLauncher,
    private val autocompleteConfig: AutocompleteAddressInteractor.Config,
    private val inlineDependencies: InlineAutocompleteDependencies?,
) : AutocompleteAddressInteractor.Factory {
    override fun create(): AutocompleteAddressInteractor {
        val dependencies = inlineDependencies
        if (dependencies != null && autocompleteConfig.isInlineAutocompleteEnabled) {
            // The shared factory can be reused while an existing form is still on screen. The
            // form/controller that created an inline interactor owns its lifecycle and disposes it
            // when that controller goes away; disposing previous interactors here can tear down the
            // active dropdown observer during unrelated form-model rebuilds.
            return BillingInlineAutocompleteAddressInteractor(
                placesClient = dependencies.placesClient,
                autocompleteConfig = autocompleteConfig,
                coroutineScope = dependencies.coroutineScope,
            )
        }
        return PaymentElementAutocompleteAddressInteractor(
            launcher = launcher,
            autocompleteConfig = autocompleteConfig,
        )
    }
}

internal data class InlineAutocompleteDependencies(
    val placesClient: PlacesClientProxy,
    val coroutineScope: CoroutineScope,
)
