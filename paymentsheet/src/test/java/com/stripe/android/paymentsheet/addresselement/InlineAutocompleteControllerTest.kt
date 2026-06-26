package com.stripe.android.paymentsheet.addresselement

import android.text.SpannableString
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.AddressComponent
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.ui.core.elements.autocomplete.model.Place
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor.InlinePredictionsState
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InlineAutocompleteControllerTest {

    @Test
    fun `initial state is Idle`() = runScenario {
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
    }

    @Test
    fun `query shorter than minimum chars stays Idle`() = runScenario {
        delegate.observeQueryChanges(queryFlow, countryFlow)

        queryFlow.value = "a"
        advanceTimeBy(500)

        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
        assertThat(fakePlacesClient.findPredictionsCalls).isEmpty()
    }

    @Test
    fun `query at minimum chars triggers fetch`() = runScenario {
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)

        queryFlow.value = "ab"
        advanceTimeBy(500)

        assertThat(fakePlacesClient.findPredictionsCalls).hasSize(1)
        assertThat(fakePlacesClient.findPredictionsCalls[0].query).isEqualTo("ab")
    }

    @Test
    fun `query with unsupported country stays Idle`() = runScenario(
        autocompleteCountries = setOf("US")
    ) {
        countryFlow.value = "CA"
        delegate.observeQueryChanges(queryFlow, countryFlow)

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
        assertThat(fakePlacesClient.findPredictionsCalls).isEmpty()
    }

    @Test
    fun `empty autocompleteCountries allows all countries`() = runScenario(
        autocompleteCountries = emptySet()
    ) {
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        countryFlow.value = "JP"
        delegate.observeQueryChanges(queryFlow, countryFlow)

        queryFlow.value = "Tokyo"
        advanceTimeBy(500)

        assertThat(fakePlacesClient.findPredictionsCalls).hasSize(1)
    }

    @Test
    fun `country check is case insensitive`() = runScenario(
        autocompleteCountries = setOf("US")
    ) {
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        countryFlow.value = "us"
        delegate.observeQueryChanges(queryFlow, countryFlow)

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        assertThat(fakePlacesClient.findPredictionsCalls).hasSize(1)
    }

    @Test
    fun `debounces rapid query changes`() = runScenario {
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)

        queryFlow.value = "12"
        advanceTimeBy(100)
        queryFlow.value = "123"
        advanceTimeBy(100)
        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        assertThat(fakePlacesClient.findPredictionsCalls).hasSize(1)
        assertThat(fakePlacesClient.findPredictionsCalls[0].query).isEqualTo("123 Main")
    }

    @Test
    fun `successful fetch sets Results state`() = runScenario {
        val predictions = listOf(
            AutocompletePrediction(
                SpannableString("123 Main St"),
                SpannableString("San Francisco, CA"),
                "place_1",
            ),
            AutocompletePrediction(
                SpannableString("456 Main Ave"),
                SpannableString("Los Angeles, CA"),
                "place_2",
            ),
        )
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(predictions)
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)

        queryFlow.value = "Main"
        advanceTimeBy(500)

        val state = delegate.inlinePredictionsState.value
        assertThat(state).isInstanceOf<InlinePredictionsState.Results>()
        val results = state as InlinePredictionsState.Results
        assertThat(results.query).isEqualTo("Main")
        assertThat(results.predictions).hasSize(2)
        assertThat(results.predictions[0].id).isEqualTo("place_1")
        assertThat(results.predictions[0].primaryText).isEqualTo("123 Main St")
        assertThat(results.predictions[0].secondaryText).isEqualTo("San Francisco, CA")
        assertThat(results.predictions[1].id).isEqualTo("place_2")
    }

    @Test
    fun `failed fetch resets to Idle`() = runScenario {
        fakePlacesClient.findPredictionsResult = Result.failure(RuntimeException("Network error"))
        delegate.observeQueryChanges(queryFlow, countryFlow)

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
    }

    @Test
    fun `state transitions to Loading before Results`() = runScenario {
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        val states = mutableListOf<InlinePredictionsState>()
        fakePlacesClient.onBeforeFindPredictions = {
            states.add(delegate.inlinePredictionsState.value)
        }
        delegate.observeQueryChanges(queryFlow, countryFlow)

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        assertThat(states).contains(InlinePredictionsState.Loading)
    }

    @Test
    fun `onPredictionSelected fetches place and emits OnValues event`() = runScenario {
        fakePlacesClient.fetchPlaceResult = Result.success(
            FetchPlaceResponse(
                Place(
                    listOf(
                        AddressComponent("123", "123", listOf(Place.Type.STREET_NUMBER.value)),
                        AddressComponent("Main St", "Main Street", listOf(Place.Type.ROUTE.value)),
                        AddressComponent("SF", "San Francisco", listOf(Place.Type.LOCALITY.value)),
                        AddressComponent(
                            "CA",
                            "California",
                            listOf(Place.Type.ADMINISTRATIVE_AREA_LEVEL_1.value)
                        ),
                        AddressComponent("US", "United States", listOf(Place.Type.COUNTRY.value)),
                        AddressComponent("94105", "94105", listOf(Place.Type.POSTAL_CODE.value)),
                    )
                )
            )
        )

        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        assertThat(fakePlacesClient.fetchPlaceCalls).containsExactly("place_1")
        assertThat(emittedEvents).hasSize(1)
        val event = emittedEvents[0]
        assertThat(event).isInstanceOf<AutocompleteAddressInteractor.Event.OnValues>()
        val values = (event as AutocompleteAddressInteractor.Event.OnValues).values
        assertThat(values[IdentifierSpec.Line1]).isEqualTo("123 Main Street")
        assertThat(values[IdentifierSpec.City]).isEqualTo("San Francisco")
        assertThat(values[IdentifierSpec.State]).isEqualTo("CA")
        assertThat(values[IdentifierSpec.Country]).isEqualTo("US")
        assertThat(values[IdentifierSpec.PostalCode]).isEqualTo("94105")
    }

    @Test
    fun `onPredictionSelected resets state to Idle`() = runScenario {
        fakePlacesClient.fetchPlaceResult = Result.success(
            FetchPlaceResponse(Place(emptyList()))
        )

        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
    }

    @Test
    fun `onPredictionSelected with failed fetch resets to Idle without emitting event`() = runScenario {
        fakePlacesClient.fetchPlaceResult = Result.failure(RuntimeException("Network error"))

        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
        assertThat(emittedEvents).isEmpty()
    }

    @Test
    fun `onPredictionSelected suppresses next query matching predicted line1`() = runScenario {
        fakePlacesClient.fetchPlaceResult = Result.success(
            FetchPlaceResponse(
                Place(
                    listOf(
                        AddressComponent("123", "123", listOf(Place.Type.STREET_NUMBER.value)),
                        AddressComponent("Main St", "Main Street", listOf(Place.Type.ROUTE.value)),
                        AddressComponent("US", "United States", listOf(Place.Type.COUNTRY.value)),
                    )
                )
            )
        )
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)

        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        queryFlow.value = "123 Main Street"
        advanceTimeBy(500)

        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
        assertThat(fakePlacesClient.findPredictionsCalls).isEmpty()
    }

    @Test
    fun `suppression only applies once - second matching query fetches normally`() = runScenario {
        fakePlacesClient.fetchPlaceResult = Result.success(
            FetchPlaceResponse(
                Place(
                    listOf(
                        AddressComponent("123", "123", listOf(Place.Type.STREET_NUMBER.value)),
                        AddressComponent("Main St", "Main Street", listOf(Place.Type.ROUTE.value)),
                        AddressComponent("US", "United States", listOf(Place.Type.COUNTRY.value)),
                    )
                )
            )
        )
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)

        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        // First matching query is suppressed
        queryFlow.value = "123 Main Street"
        advanceTimeBy(500)
        assertThat(fakePlacesClient.findPredictionsCalls).isEmpty()

        // Second matching query fetches normally
        queryFlow.value = "123 Main Street "
        advanceTimeBy(500)
        queryFlow.value = "123 Main Street"
        advanceTimeBy(500)
        assertThat(fakePlacesClient.findPredictionsCalls).hasSize(2)
    }

    @Test
    fun `onDismissed resets state to Idle`() = runScenario {
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(
                listOf(
                    AutocompletePrediction(
                        SpannableString("123 Main"),
                        SpannableString("SF"),
                        "place_1"
                    )
                )
            )
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)
        queryFlow.value = "123 Main"
        advanceTimeBy(500)
        assertThat(delegate.inlinePredictionsState.value)
            .isInstanceOf<InlinePredictionsState.Results>()

        delegate.onDismissed()

        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
    }

    @Test
    fun `null placesClient results in no fetches and stays Idle`() = runScenario(
        usePlacesClient = false
    ) {
        delegate.observeQueryChanges(queryFlow, countryFlow)

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
    }

    @Test
    fun `null placesClient on prediction selected does not crash`() = runScenario(
        usePlacesClient = false
    ) {
        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
        assertThat(emittedEvents).isEmpty()
    }

    @Test
    fun `country change triggers re-evaluation`() = runScenario(
        autocompleteCountries = setOf("US")
    ) {
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        countryFlow.value = "CA"
        delegate.observeQueryChanges(queryFlow, countryFlow)

        queryFlow.value = "123 Main"
        advanceTimeBy(500)
        assertThat(fakePlacesClient.findPredictionsCalls).isEmpty()

        countryFlow.value = "US"
        advanceTimeBy(500)
        assertThat(fakePlacesClient.findPredictionsCalls).hasSize(1)
    }

    @Test
    fun `calling observeQueryChanges again cancels previous observation`() = runScenario {
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        val secondQueryFlow = MutableStateFlow("")
        val secondCountryFlow = MutableStateFlow<String?>("US")

        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.observeQueryChanges(secondQueryFlow, secondCountryFlow)

        queryFlow.value = "first query"
        advanceTimeBy(500)
        assertThat(fakePlacesClient.findPredictionsCalls).isEmpty()

        secondQueryFlow.value = "second query"
        advanceTimeBy(500)
        assertThat(fakePlacesClient.findPredictionsCalls).hasSize(1)
        assertThat(fakePlacesClient.findPredictionsCalls[0].query).isEqualTo("second query")
    }

    @Test
    fun `null country flow value defaults to empty string`() = runScenario(
        autocompleteCountries = emptySet()
    ) {
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        countryFlow.value = null
        delegate.observeQueryChanges(queryFlow, countryFlow)

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        assertThat(fakePlacesClient.findPredictionsCalls[0].country).isEqualTo("")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun runScenario(
        autocompleteCountries: Set<String> = emptySet(),
        usePlacesClient: Boolean = true,
        block: suspend Scenario.() -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        val fakePlaces = if (usePlacesClient) FakePlacesClientProxy() else null
        val emittedEvents = mutableListOf<AutocompleteAddressInteractor.Event>()
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "test_key",
            autocompleteCountries = autocompleteCountries,
            isPlacesAvailable = true,
            isInlineAutocompleteEnabled = true,
        )
        val delegate = InlineAutocompleteController(
            placesClient = fakePlaces,
            config = config,
            coroutineScope = backgroundScope,
            eventListenerProvider = { { event -> emittedEvents.add(event) } },
        )

        Scenario(
            delegate = delegate,
            fakePlacesClient = fakePlaces ?: FakePlacesClientProxy(),
            emittedEvents = emittedEvents,
            queryFlow = MutableStateFlow(""),
            countryFlow = MutableStateFlow("US"),
            testScope = this,
        ).apply { block() }
    }

    private class Scenario(
        val delegate: InlineAutocompleteController,
        val fakePlacesClient: FakePlacesClientProxy,
        val emittedEvents: MutableList<AutocompleteAddressInteractor.Event>,
        val queryFlow: MutableStateFlow<String>,
        val countryFlow: MutableStateFlow<String?>,
        val testScope: TestScope,
    ) {
        fun advanceTimeBy(millis: Long) = testScope.advanceTimeBy(millis)
    }

    private class FakePlacesClientProxy : PlacesClientProxy {
        var findPredictionsResult: Result<FindAutocompletePredictionsResponse> =
            Result.success(FindAutocompletePredictionsResponse(emptyList()))
        var fetchPlaceResult: Result<FetchPlaceResponse> =
            Result.success(FetchPlaceResponse(Place(emptyList())))
        var onBeforeFindPredictions: (() -> Unit)? = null

        data class FindPredictionsCall(val query: String?, val country: String, val limit: Int)

        val findPredictionsCalls = mutableListOf<FindPredictionsCall>()
        val fetchPlaceCalls = mutableListOf<String>()

        override suspend fun findAutocompletePredictions(
            query: String?,
            country: String,
            limit: Int
        ): Result<FindAutocompletePredictionsResponse> {
            onBeforeFindPredictions?.invoke()
            findPredictionsCalls.add(FindPredictionsCall(query, country, limit))
            return findPredictionsResult
        }

        override suspend fun fetchPlace(placeId: String): Result<FetchPlaceResponse> {
            fetchPlaceCalls.add(placeId)
            return fetchPlaceResult
        }
    }
}
