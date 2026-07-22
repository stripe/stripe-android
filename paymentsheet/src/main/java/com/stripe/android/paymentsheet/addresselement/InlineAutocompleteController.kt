package com.stripe.android.paymentsheet.addresselement

import androidx.appcompat.app.AppCompatDelegate
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.util.Locale

internal class InlineAutocompleteController(
    private val placesClient: PlacesClientProxy,
    private val config: AutocompleteAddressInteractor.Config,
    private val coroutineScope: CoroutineScope,
    private val eventListenerProvider: () -> ((AutocompleteAddressInteractor.Event) -> Unit)?,
) {
    private var lastPredictionLine1: String? = null
    private var latestQuery: String? = null
    private var latestCountry: String? = null
    private var observeJob: Job? = null
    private var selectionJob: Job? = null

    private val _inlinePredictionsState = MutableStateFlow<AutocompleteAddressInteractor.InlinePredictionsState>(
        AutocompleteAddressInteractor.InlinePredictionsState.Idle
    )
    val inlinePredictionsState: StateFlow<AutocompleteAddressInteractor.InlinePredictionsState> =
        _inlinePredictionsState.asStateFlow()

    @OptIn(FlowPreview::class)
    fun observeQueryChanges(query: StateFlow<String>, country: StateFlow<String?>) {
        observeJob?.cancel()
        observeJob = coroutineScope.launch {
            combine(query, country) { q, c -> q to (c ?: "") }
                .debounce(AutocompleteViewModel.SEARCH_DEBOUNCE_MS)
                .collectLatest { (q, c) ->
                    if (q == lastPredictionLine1) {
                        lastPredictionLine1 = null
                        _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Idle
                        return@collectLatest
                    }
                    if (q.length < AutocompleteViewModel.MIN_CHARS_AUTOCOMPLETE || !isCountrySupported(c)) {
                        lastPredictionLine1 = null
                        _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Idle
                        return@collectLatest
                    }
                    latestQuery = q
                    latestCountry = c
                    fetchPredictions(q, c)
                }
        }
    }

    fun onPredictionSelected(predictionId: String) {
        selectionJob?.cancel()
        selectionJob = coroutineScope.launch {
            val result = placesClient.fetchPlace(predictionId)
            ensureActive()
            result.fold(
                onSuccess = { handleFetchPlaceSuccess(it) },
                onFailure = { handleFailure() }
            )
            placesClient.resetSession()
        }
    }

    private fun handleFetchPlaceSuccess(response: FetchPlaceResponse) {
        val locale = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
        val address = placesClient.transformToAddress(response, locale)
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
    }

    fun onDismissed() {
        selectionJob?.cancel()
        lastPredictionLine1 = null
        latestQuery = null
        latestCountry = null
        _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Idle
    }

    fun dispose() {
        observeJob?.cancel()
        selectionJob?.cancel()
    }

    private fun isCountrySupported(country: String): Boolean {
        val supportedCountries = config.autocompleteCountries
        return supportedCountries.isEmpty() ||
            supportedCountries.any { it.equals(country, ignoreCase = true) }
    }

    private suspend fun fetchPredictions(query: String, country: String) {
        _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Loading
        val result = placesClient.findAutocompletePredictions(
            query = query,
            country = country,
            limit = AutocompleteViewModel.MAX_DISPLAYED_RESULTS,
        )
        currentCoroutineContext().ensureActive()
        result.fold(
            onSuccess = { handleFindPredictionsSuccess(query, it) },
            onFailure = { handleFailure() }
        )
    }

    private fun handleFindPredictionsSuccess(
        query: String,
        response: FindAutocompletePredictionsResponse,
    ) {
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
    }

    private fun handleFailure() {
        _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Idle
        if (config.shouldUseStripeHostedAutocomplete) {
            expandFormForHostedFailure()
        }
    }

    private fun expandFormForHostedFailure() {
        val values = buildMap<IdentifierSpec, String?> {
            latestQuery?.takeIf { it.isNotBlank() }?.let { put(IdentifierSpec.Line1, it) }
            latestCountry?.takeIf { it.isNotBlank() }?.let { put(IdentifierSpec.Country, it) }
        }.takeIf { it.isNotEmpty() }

        eventListenerProvider()?.invoke(
            AutocompleteAddressInteractor.Event.OnExpandForm(values)
        )
    }
}
