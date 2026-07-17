package com.stripe.android.paymentsheet.addresselement

import androidx.appcompat.app.AppCompatDelegate
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
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
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

internal class InlineAutocompleteController(
    private val placesClient: PlacesClientProxy?,
    private val config: AutocompleteAddressInteractor.Config,
    private val coroutineScope: CoroutineScope,
    private val eventListenerProvider: () -> ((AutocompleteAddressInteractor.Event) -> Unit)?,
    private val shouldUseAutocompleteProxyEndpoints: Boolean,
    private val stripeAutocompleteApiService: StripeAutocompleteApiService?,
) {
    private var lastPredictionLine1: String? = null
    private var latestQuery: String? = null
    private var latestCountry: String? = null
    private var observeJob: Job? = null
    private var selectionJob: Job? = null
    private var sessionToken: String = UUID.randomUUID().toString()
    private val cachedAddresses = mutableMapOf<String, StripeProxyAddress>()

    private val _inlinePredictionsState = MutableStateFlow<AutocompleteAddressInteractor.InlinePredictionsState>(
        AutocompleteAddressInteractor.InlinePredictionsState.Idle
    )
    val inlinePredictionsState: StateFlow<AutocompleteAddressInteractor.InlinePredictionsState> =
        _inlinePredictionsState.asStateFlow()

    private val canUseAutocompleteProxyEndpoints: Boolean
        get() = shouldUseAutocompleteProxyEndpoints && stripeAutocompleteApiService != null

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
            if (canUseAutocompleteProxyEndpoints) {
                fetchPlaceFromStripeProxy(predictionId)
            } else {
                fetchPlaceFromGooglePlaces(predictionId)
            }
        }
    }

    private suspend fun fetchPlaceFromStripeProxy(predictionId: String) {
        val cachedAddress = cachedAddresses[predictionId]
        if (cachedAddress != null) {
            emitAddressFromProxy(cachedAddress)
            resetSession()
            return
        }
        val service = stripeAutocompleteApiService ?: return
        service.fetchPlaceDetails(
            placeId = predictionId,
            sessionToken = sessionToken,
        ).fold(
            onSuccess = { result ->
                emitAddressFromProxy(result.address)
                resetSession()
            },
            onFailure = { handleFailure() }
        )
    }

    private fun emitAddressFromProxy(address: StripeProxyAddress) {
        emitSelectedAddress(
            line1 = address.line1,
            line2 = address.line2,
            city = address.city,
            state = address.state,
            postalCode = address.postalCode,
            country = address.country,
        )
    }

    private suspend fun fetchPlaceFromGooglePlaces(predictionId: String) {
        val client = placesClient ?: return
        client.fetchPlace(predictionId).fold(
            onSuccess = ::handleFetchPlaceSuccess,
            onFailure = { handleFailure() }
        )
    }

    private fun handleFetchPlaceSuccess(response: FetchPlaceResponse) {
        val locale = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
        val address = response.place.transformGoogleToStripeAddress(locale)
        emitSelectedAddress(
            line1 = address.line1,
            line2 = address.line2,
            city = address.city,
            state = address.state,
            postalCode = address.postalCode,
            country = address.country,
        )
    }

    private fun emitSelectedAddress(
        line1: String?,
        line2: String?,
        city: String?,
        state: String?,
        postalCode: String?,
        country: String?,
    ) {
        lastPredictionLine1 = line1
        _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Idle
        eventListenerProvider()?.invoke(
            AutocompleteAddressInteractor.Event.OnValues(
                mapOf(
                    IdentifierSpec.Line1 to line1,
                    IdentifierSpec.Line2 to line2,
                    IdentifierSpec.City to city,
                    IdentifierSpec.State to state,
                    IdentifierSpec.PostalCode to postalCode,
                    IdentifierSpec.Country to country,
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
        resetSession()
    }

    private fun resetSession() {
        sessionToken = UUID.randomUUID().toString()
        cachedAddresses.clear()
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
        if (canUseAutocompleteProxyEndpoints) {
            fetchPredictionsFromStripeProxy(query, country)
        } else {
            fetchPredictionsFromGooglePlaces(query, country)
        }
    }

    private suspend fun fetchPredictionsFromStripeProxy(query: String, country: String) {
        val service = stripeAutocompleteApiService ?: return
        _inlinePredictionsState.value =
            AutocompleteAddressInteractor.InlinePredictionsState.Loading
        val locale = AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()
            ?: Locale.getDefault().toLanguageTag()
        service.findAutocompletePredictions(
            query = query,
            country = country,
            sessionToken = sessionToken,
            locale = locale,
            googleApiKey = config.googlePlacesApiKey,
        ).fold(
            onSuccess = { result ->
                cachedAddresses.clear()
                result.predictions.forEach { suggestion ->
                    suggestion.address?.let { cachedAddresses[suggestion.placeId] = it }
                }
                _inlinePredictionsState.value =
                    AutocompleteAddressInteractor.InlinePredictionsState.Results(
                        query = query,
                        predictions = result.predictions.map { suggestion ->
                            AutocompleteAddressInteractor.InlineAddressPrediction(
                                id = suggestion.placeId,
                                primaryText = suggestion.primaryText,
                                secondaryText = suggestion.secondaryText,
                            )
                        }
                    )
            },
            onFailure = { handleFailure() }
        )
    }

    private suspend fun fetchPredictionsFromGooglePlaces(query: String, country: String) {
        val client = placesClient ?: return
        _inlinePredictionsState.value = AutocompleteAddressInteractor.InlinePredictionsState.Loading
        val result = client.findAutocompletePredictions(
            query = query,
            country = country,
            limit = AutocompleteViewModel.MAX_DISPLAYED_RESULTS,
        )
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
