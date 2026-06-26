package com.stripe.android.paymentsheet.addresselement

import androidx.appcompat.app.AppCompatDelegate
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.transformGoogleToStripeAddress
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.Locale

internal class InlineAutocompleteController(
    private val placesClient: PlacesClientProxy?,
    private val config: AutocompleteAddressInteractor.Config,
    private val coroutineScope: CoroutineScope,
    private val eventListenerProvider: () -> ((AutocompleteAddressInteractor.Event) -> Unit)?,
) {
    private var lastPredictionLine1: String? = null
    private var observeJob: Job? = null

    private val _inlinePredictionsState = MutableStateFlow<AutocompleteAddressInteractor.InlinePredictionsState>(
        AutocompleteAddressInteractor.InlinePredictionsState.Idle
    )
    val inlinePredictionsState: StateFlow<AutocompleteAddressInteractor.InlinePredictionsState> =
        _inlinePredictionsState.asStateFlow()

    @OptIn(FlowPreview::class)
    fun observeQueryChanges(query: StateFlow<String>, country: StateFlow<String?>) {
        if (placesClient == null) return
        observeJob?.cancel()
        observeJob = coroutineScope.launch {
            combine(query, country) { q, c -> q to (c ?: "") }
                .drop(1)
                .debounce(SEARCH_DEBOUNCE_MS)
                .collectLatest { (q, c) ->
                    if (q == lastPredictionLine1) {
                        lastPredictionLine1 = null
                        _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Idle
                        return@collectLatest
                    }
                    if (q.length < MIN_CHARS_AUTOCOMPLETE || !isCountrySupported(c)) {
                        _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Idle
                        return@collectLatest
                    }
                    fetchPredictions(q, c)
                }
        }
    }

    fun onPredictionSelected(predictionId: String) {
        if (placesClient == null) return
        coroutineScope.launch {
            placesClient.fetchPlace(predictionId).fold(
                onSuccess = { response ->
                    val locale = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
                    val address = response.place.transformGoogleToStripeAddress(locale)
                    lastPredictionLine1 = address.line1
                    _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Idle
                    eventListenerProvider()?.invoke(
                        AutocompleteAddressInteractor.Event.OnValues(
                            mapOf(
                                IdentifierSpec.Line1 to address.line1,
                                IdentifierSpec.Line2 to address.line2,
                                IdentifierSpec.City to address.city,
                                IdentifierSpec.State to address.state,
                                IdentifierSpec.PostalCode to address.postalCode,
                                IdentifierSpec.Country to address.country,
                            )
                        )
                    )
                },
                onFailure = {
                    _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Idle
                }
            )
        }
    }

    fun onDismissed() {
        _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Idle
    }

    private fun isCountrySupported(country: String): Boolean {
        val supportedCountries = config.autocompleteCountries
        return supportedCountries.isEmpty() ||
            supportedCountries.any { it.equals(country, ignoreCase = true) }
    }

    private suspend fun fetchPredictions(query: String, country: String) {
        _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Loading
        val result = placesClient?.findAutocompletePredictions(
            query = query,
            country = country,
            limit = MAX_DISPLAYED_RESULTS,
        ) ?: return
        result.fold(
            onSuccess = { response ->
                _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Results(
                    query = query,
                    predictions = response.autocompletePredictions.map { prediction ->
                        AutocompleteAddressInteractor.InlineAddressPrediction(
                            id = prediction.placeId,
                            primaryText = prediction.primaryText.toString(),
                            secondaryText = prediction.secondaryText.toString(),
                        )
                    }
                )
            },
            onFailure = {
                _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Idle
            }
        )
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
        const val MAX_DISPLAYED_RESULTS = 4
        const val MIN_CHARS_AUTOCOMPLETE = 2
    }
}
