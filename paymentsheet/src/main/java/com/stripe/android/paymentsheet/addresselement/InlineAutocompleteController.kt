package com.stripe.android.paymentsheet.addresselement

import androidx.appcompat.app.AppCompatDelegate
import com.stripe.android.model.Address
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor.InlinePredictionsState
import com.stripe.android.uicore.elements.FormFieldId
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
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

    // The id keeps a repeated Search or selection distinct, so it restarts its lane.
    private data class PredictionRequest(val query: String, val country: String, val id: Long) {
        fun matches(query: String, country: String) = this.query == query && this.country == country
    }

    private data class SelectionRequest(val placeId: String, val id: Long)

    // Each lane runs the work for its current request, so replacing or clearing a request cancels that work.
    private data class State(
        val activation: ActivationState,
        val paused: Boolean,
        val prediction: PredictionRequest?,
        val selection: SelectionRequest?,
        val display: InlinePredictionsState,
    )

    private val state = MutableStateFlow(
        State(
            activation = ActivationState.ActiveByQuery,
            paused = true,
            prediction = null,
            selection = null,
            display = InlinePredictionsState.Idle,
        )
    )
    private var nextRequestId = 0L
    private var lastObservedCountry: String? = null
    private var queryFlow: StateFlow<String>? = null
    private var countryFlow: StateFlow<String?>? = null
    private var observeJob: Job? = null
    var autocompleteFilledAddress: Address? = null
        private set

    val inlinePredictionsState: StateFlow<InlinePredictionsState> = state.mapAsStateFlow { it.display }

    // collectLatest waits for a cancelled fetch to finish, which also keeps an old selection's
    // resetSession() ahead of the next fetchPlace().
    private val lanes: Job = coroutineScope.launch {
        launch {
            state.map { it.prediction }.distinctUntilChanged().collectLatest { request ->
                request?.let { fetchPredictions(it) }
            }
        }
        state.map { it.selection }.distinctUntilChanged().collectLatest { request ->
            request?.let { fetchSelectedPlace(it) }
        }
    }

    @OptIn(FlowPreview::class)
    fun observeQueryChanges(query: StateFlow<String>, country: StateFlow<String?>) {
        observeJob?.cancel()
        queryFlow = query
        countryFlow = country
        lastObservedCountry = country.value ?: ""
        state.update { it.copy(prediction = null) }
        observeJob = coroutineScope.launch {
            combine(query, country, state.map { it.paused }.distinctUntilChanged()) { q, c, _ ->
                q to (c ?: "")
            }
                .onEach { (q, c) -> onInputChanged(q, c) }
                .debounce(AutocompleteViewModel.SEARCH_DEBOUNCE_MS)
                .collect { (q, c) -> onInputSettled(q, c) }
        }
    }

    private fun onInputChanged(query: String, country: String) {
        state.update { current ->
            if (current.prediction?.matches(query, country) == false) {
                current.copy(prediction = null)
            } else {
                current
            }
        }
    }

    private fun onInputSettled(query: String, country: String) {
        val countryChanged = country != lastObservedCountry
        if (countryChanged) {
            lastObservedCountry = country
            if (!isCountrySupported(country)) {
                state.update { it.idle() }
                emitCountry(country)
                return
            }
        }
        val current = state.value
        if (!current.isEligible(query, country)) {
            state.update { it.idle() }
            if (countryChanged && current.activation != ActivationState.Inactive) {
                emitCountry(country)
            }
            return
        }
        // Search may have already fetched or be fetching this query inside the debounce window.
        if (current.prediction?.matches(query, country) == true) return
        val request = PredictionRequest(query, country, nextRequestId++)
        state.update { it.withPrediction(request) }
    }

    fun onSearchActivated() {
        state.update { it.copy(paused = false, activation = ActivationState.ActiveBySearch) }
        val q = queryFlow?.value ?: return
        val c = countryFlow?.value ?: ""
        if (!state.value.isEligible(q, c)) return
        val request = PredictionRequest(q, c, nextRequestId++)
        state.update { it.copy(selection = null).withPrediction(request) }
    }

    fun onPredictionSelected(predictionId: String) {
        val request = SelectionRequest(predictionId, nextRequestId++)
        state.update { it.copy(prediction = null, selection = request) }
    }

    fun onDismissed() {
        state.update {
            it.idle().copy(
                activation = if (it.activation == ActivationState.ActiveBySearch) {
                    ActivationState.Inactive
                } else {
                    it.activation
                },
                selection = null,
            )
        }
    }

    fun onFocusLost() {
        // Pause prediction fetching, but keep observing the country flow so switching between
        // supported and unsupported countries still toggles the form's mode while the inline
        // field is unfocused (e.g. after the form has expanded to manual entry).
        state.update { it.idle().copy(paused = true) }
    }

    fun onFocusGained() {
        state.update { it.copy(paused = false) }
    }

    fun expandFormFromInline() {
        state.update { it.idle().copy(activation = ActivationState.Inactive, selection = null) }
        emitExpandForm(
            query = queryFlow?.value,
            country = countryFlow?.value,
        )
    }

    // Terminal: the lanes stop, so a disposed controller is not reused.
    fun dispose() {
        observeJob?.cancel()
        lanes.cancel()
        state.update { it.idle().copy(selection = null) }
    }

    private fun State.isEligible(query: String, country: String): Boolean {
        return !paused &&
            activation != ActivationState.Inactive &&
            isCountrySupported(country) &&
            query.length >= AutocompleteViewModel.MIN_CHARS_AUTOCOMPLETE
    }

    private fun State.idle() = copy(prediction = null, display = InlinePredictionsState.Idle)

    private fun State.withPrediction(request: PredictionRequest) = copy(
        prediction = request,
        display = display as? InlinePredictionsState.Results ?: InlinePredictionsState.Loading,
    )

    private fun isCountrySupported(country: String): Boolean {
        val supportedCountries = config.autocompleteCountries
        return supportedCountries.isEmpty() ||
            supportedCountries.any { it.equals(country, ignoreCase = true) }
    }

    /**
     * Applies [transform] only while [isCurrent] still holds, so results for a replaced or
     * cleared request are dropped.
     */
    private inline fun updateIfCurrent(isCurrent: (State) -> Boolean, transform: (State) -> State): Boolean {
        while (true) {
            val current = state.value
            if (!isCurrent(current)) return false
            if (state.compareAndSet(current, transform(current))) return true
        }
    }

    private suspend fun fetchPredictions(request: PredictionRequest) {
        val result = placesClient.findAutocompletePredictions(
            query = request.query,
            country = request.country,
            limit = AutocompleteViewModel.MAX_DISPLAYED_RESULTS,
        )
        val predictions = result.fold(
            onSuccess = { response ->
                response.autocompletePredictions.map { prediction ->
                    AutocompleteAddressInteractor.InlineAddressPrediction(
                        id = prediction.placeId,
                        primaryText = prediction.primaryText.toString(),
                        secondaryText = prediction.secondaryText.toString(),
                    )
                }
            },
            onFailure = { emptyList() },
        )
        updateIfCurrent({ it.prediction == request }) {
            it.copy(display = InlinePredictionsState.Results(query = request.query, predictions = predictions))
        }
    }

    private suspend fun fetchSelectedPlace(request: SelectionRequest) {
        val locale = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
        try {
            placesClient.fetchPlace(request.placeId, locale).fold(
                onSuccess = { handleFetchPlaceSuccess(request, it) },
                onFailure = { handleFailure(request, queryFlow?.value, countryFlow?.value) }
            )
        } finally {
            placesClient.resetSession()
        }
    }

    private fun handleFetchPlaceSuccess(request: SelectionRequest, address: Address) {
        val applied = updateIfCurrent({ it.selection == request }) {
            it.idle().copy(activation = ActivationState.Inactive)
        }
        if (!applied) return
        autocompleteFilledAddress = address
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

    private fun handleFailure(request: SelectionRequest, query: String?, country: String?) {
        val shouldExpand = config.shouldUseStripeHostedAutocomplete
        val applied = updateIfCurrent({ it.selection == request }) {
            it.copy(
                activation = if (shouldExpand) ActivationState.Inactive else it.activation,
                display = InlinePredictionsState.Results(query = query ?: "", predictions = emptyList()),
            )
        }
        if (applied && shouldExpand) {
            emitExpandForm(query = query, country = country)
        }
    }

    private fun emitCountry(country: String) {
        eventListenerProvider()?.invoke(
            AutocompleteAddressInteractor.Event.OnValues(mapOf(FormFieldId.Country to country))
        )
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
