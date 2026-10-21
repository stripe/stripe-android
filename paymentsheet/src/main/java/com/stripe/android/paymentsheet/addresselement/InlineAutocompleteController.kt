package com.stripe.android.paymentsheet.addresselement

import androidx.appcompat.app.AppCompatDelegate
import com.stripe.android.model.Address
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.FormFieldId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
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
    private enum class ActivationState {
        ActiveByQuery, ActiveBySearch, Inactive
    }

    private class PredictionWork(val job: Job, val query: Pair<String, String>)

    private var activationState: ActivationState = ActivationState.ActiveByQuery
    private var lastObservedCountry: String? = null
    private var predictionsPaused = false
    private var queryFlow: StateFlow<String>? = null
    private var countryFlow: StateFlow<String?>? = null
    private var observeJob: Job? = null
    private var predictionWork: PredictionWork? = null
    private var selectionJob: Job? = null
    var autocompleteFilledAddress: Address? = null
        private set

    private val _inlinePredictionsState = MutableStateFlow<AutocompleteAddressInteractor.InlinePredictionsState>(
        AutocompleteAddressInteractor.InlinePredictionsState.Idle
    )
    val inlinePredictionsState: StateFlow<AutocompleteAddressInteractor.InlinePredictionsState> =
        _inlinePredictionsState.asStateFlow()

    @OptIn(FlowPreview::class)
    fun observeQueryChanges(query: StateFlow<String>, country: StateFlow<String?>) {
        observeJob?.cancel()
        cancelPredictionWork()
        queryFlow = query
        countryFlow = country
        lastObservedCountry = country.value ?: ""
        predictionsPaused = false
        observeJob = coroutineScope.launch {
            combine(query, country) { q, c -> q to (c ?: "") }
                .debounce(AutocompleteViewModel.SEARCH_DEBOUNCE_MS)
                .collect { (q, c) -> onQueryChanged(q, c) }
        }
    }

    private fun onQueryChanged(query: String, country: String) {
        val countryChanged = country != lastObservedCountry
        if (countryChanged) {
            lastObservedCountry = country
            if (!isCountrySupported(country)) {
                cancelPredictionWork()
                _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Idle
                eventListenerProvider()?.invoke(
                    AutocompleteAddressInteractor.Event.OnValues(mapOf(FormFieldId.Country to country))
                )
                return
            }
        }
        if (!isQueryEligible(query, country)) {
            cancelPredictionWork()
            _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Idle
            if (countryChanged && activationState != ActivationState.Inactive) {
                eventListenerProvider()?.invoke(
                    AutocompleteAddressInteractor.Event.OnValues(mapOf(FormFieldId.Country to country))
                )
            }
            return
        }
        requestPredictions(query, country, isExplicitSearch = false)
    }

    fun onSearchActivated() {
        predictionsPaused = false
        activationState = ActivationState.ActiveBySearch
        val q = queryFlow?.value ?: return
        val c = countryFlow?.value ?: ""
        requestPredictions(q, c, isExplicitSearch = true)
    }

    private fun requestPredictions(query: String, country: String, isExplicitSearch: Boolean) {
        if (!isQueryEligible(query, country)) return
        if (!isExplicitSearch && predictionWork?.query == (query to country)) return
        cancelPredictionWork()
        if (isExplicitSearch) {
            cancelSelectionWork()
        }
        val job = coroutineScope.launch(start = CoroutineStart.LAZY) { fetchPredictions(query, country) }
        predictionWork = PredictionWork(job, query to country)
        job.start()
    }

    fun onPredictionSelected(predictionId: String) {
        cancelPredictionWork()
        cancelSelectionWork()
        val job = coroutineScope.launch(start = CoroutineStart.LAZY) {
            val locale = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
            try {
                val result = placesClient.fetchPlace(predictionId, locale)
                ensureActive()
                result.fold(
                    onSuccess = { handleFetchPlaceSuccess(it) },
                    onFailure = { handleFailure(queryFlow?.value, countryFlow?.value) }
                )
            } finally {
                placesClient.resetSession()
            }
        }
        selectionJob = job
        job.start()
    }

    private fun handleFetchPlaceSuccess(address: Address) {
        activationState = ActivationState.Inactive
        autocompleteFilledAddress = address
        _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Idle
        eventListenerProvider()?.invoke(
            AutocompleteAddressInteractor.Event.OnExpandForm(
                mapOf(
                    FormFieldId.Line1 to address.line1,
                    FormFieldId.Line2 to address.line2,
                    FormFieldId.City to address.city,
                    FormFieldId.State to address.state,
                    FormFieldId.PostalCode to address.postalCode,
                    FormFieldId.Country to address.country,
                )
            )
        )
    }

    fun onDismissed() {
        cancelPredictionWork()
        cancelSelectionWork()
        _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Idle
        if (activationState == ActivationState.ActiveBySearch) {
            activationState = ActivationState.Inactive
        }
    }

    fun onFocusLost() {
        // Pause prediction fetching, but keep observing the country flow so switching between
        // supported and unsupported countries still toggles the form's mode while the inline
        // field is unfocused (e.g. after the form has expanded to manual entry).
        predictionsPaused = true
        cancelPredictionWork()
        _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Idle
    }

    fun onFocusGained() {
        predictionsPaused = false
        if (observeJob?.isActive == true) return
        val q = queryFlow ?: return
        val c = countryFlow ?: return
        observeQueryChanges(q, c)
    }

    fun expandFormFromInline() {
        activationState = ActivationState.Inactive
        cancelPredictionWork()
        cancelSelectionWork()
        _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Idle
        emitExpandForm(
            query = queryFlow?.value,
            country = countryFlow?.value,
        )
    }

    fun dispose() {
        observeJob?.cancel()
        cancelPredictionWork()
        cancelSelectionWork()
        _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Idle
    }

    private fun cancelPredictionWork() {
        predictionWork?.job?.cancel()
        predictionWork = null
    }

    private fun cancelSelectionWork() {
        selectionJob?.cancel()
        selectionJob = null
    }

    private fun isQueryEligible(query: String, country: String): Boolean {
        return !predictionsPaused &&
            activationState != ActivationState.Inactive &&
            isCountrySupported(country) &&
            query.length >= AutocompleteViewModel.MIN_CHARS_AUTOCOMPLETE
    }

    private fun isCountrySupported(country: String): Boolean {
        val supportedCountries = config.autocompleteCountries
        return supportedCountries.isEmpty() ||
            supportedCountries.any { it.equals(country, ignoreCase = true) }
    }

    private suspend fun fetchPredictions(query: String, country: String) {
        if (_inlinePredictionsState.value !is AutocompleteAddressInteractor.InlinePredictionsState.Results) {
            _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Loading
        }
        val result = placesClient.findAutocompletePredictions(
            query = query,
            country = country,
            limit = AutocompleteViewModel.MAX_DISPLAYED_RESULTS,
        )
        currentCoroutineContext().ensureActive()
        if (_inlinePredictionsState.value == AutocompleteAddressInteractor.InlinePredictionsState.Idle) return
        if (queryFlow?.value != query) return
        if ((countryFlow?.value ?: "") != country) return
        result.fold(
            onSuccess = { handleFindPredictionsSuccess(query, it) },
            onFailure = {
                _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Results(
                    query = query,
                    predictions = emptyList(),
                )
            }
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

    private fun handleFailure(query: String?, country: String?) {
        _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Results(
            query = query ?: "",
            predictions = emptyList(),
        )
        if (config.shouldUseStripeHostedAutocomplete) {
            activationState = ActivationState.Inactive
            emitExpandForm(query = query, country = country)
        }
    }

    private fun emitExpandForm(query: String?, country: String?) {
        val values = buildMap<FormFieldId, String?> {
            query?.takeIf { it.isNotBlank() }?.let { put(FormFieldId.Line1, it) }
            country?.takeIf { it.isNotBlank() }?.let { put(FormFieldId.Country, it) }
        }.takeIf { it.isNotEmpty() }

        eventListenerProvider()?.invoke(
            AutocompleteAddressInteractor.Event.OnExpandForm(values)
        )
    }
}
