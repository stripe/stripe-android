package com.stripe.android.paymentsheet.addresselement

import android.text.SpannableString
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.model.Address
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor.InlinePredictionsState
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Suppress("LargeClass")
@RunWith(RobolectricTestRunner::class)
class InlineAutocompleteControllerTest {

    @Test
    fun `initial state is Idle`() = runScenario {
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
    }

    @Test
    fun `query shorter than minimum chars stays Idle`() = runScenario {
        delegate.observeQueryChanges(queryFlow, countryFlow)

        queryFlow.value = "ab"
        advanceTimeBy(500)

        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
    }

    @Test
    fun `query at minimum chars triggers fetch when active`() = runScenario {
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "abc"
        advanceTimeBy(500)

        val call = fakePlacesClient.findPredictionsCalls.awaitItem()
        assertThat(call.query).isEqualTo("abc")
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
    }

    @Test
    fun `onSearchActivated with unsupported country does not fetch`() = runScenario(
        autocompleteCountries = setOf("US")
    ) {
        countryFlow.value = "CA"
        delegate.observeQueryChanges(queryFlow, countryFlow)
        queryFlow.value = "123 Main"

        delegate.onSearchActivated()
        advanceTimeBy(500)

        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
        fakePlacesClient.findPredictionsCalls.expectNoEvents()
    }

    @Test
    fun `switching to unsupported country emits OnValues event`() = runScenario(
        autocompleteCountries = setOf("US")
    ) {
        delegate.observeQueryChanges(queryFlow, countryFlow)

        queryFlow.value = "123 Main"
        countryFlow.value = "CA"
        advanceTimeBy(500)

        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
        val event = eventCalls.awaitItem()
        assertThat(event).isEqualTo(
            AutocompleteAddressInteractor.Event.OnValues(
                mapOf(IdentifierSpec.Country to "CA")
            )
        )
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
        delegate.onSearchActivated()

        queryFlow.value = "Tokyo"
        advanceTimeBy(500)

        fakePlacesClient.findPredictionsCalls.awaitItem()
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
        delegate.onSearchActivated()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        fakePlacesClient.findPredictionsCalls.awaitItem()
    }

    @Test
    fun `debounces rapid query changes`() = runScenario {
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "12"
        advanceTimeBy(100)
        queryFlow.value = "123"
        advanceTimeBy(100)
        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        val call = fakePlacesClient.findPredictionsCalls.awaitItem()
        assertThat(call.query).isEqualTo("123 Main")
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
        delegate.onSearchActivated()

        queryFlow.value = "Main"
        advanceTimeBy(500)

        fakePlacesClient.findPredictionsCalls.awaitItem()
        val state = delegate.inlinePredictionsState.value
        assertThat(state).isInstanceOf<InlinePredictionsState.Results>()
        val results = state as InlinePredictionsState.Results
        assertThat(results.query).isEqualTo("Main")
        assertThat(results.predictions).hasSize(2)
        assertThat(results.predictions[0].id).isEqualTo("place_1")
        assertThat(results.predictions[0].primaryText).isEqualTo("123 Main St")
        assertThat(results.predictions[0].secondaryText)
            .isEqualTo("San Francisco, CA")
        assertThat(results.predictions[1].id).isEqualTo("place_2")
    }

    @Test
    fun `failed fetch keeps dropdown open with empty results`() = runScenario {
        fakePlacesClient.findPredictionsResult = Result.failure(RuntimeException("Network error"))
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        fakePlacesClient.findPredictionsCalls.awaitItem()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(
            InlinePredictionsState.Results(query = "123 Main", predictions = emptyList())
        )
    }

    @Test
    fun `state transitions to Loading before Results`() = runScenario {
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        var stateBeforeFetch: InlinePredictionsState? = null
        fakePlacesClient.onBeforeFindPredictions = {
            stateBeforeFetch = delegate.inlinePredictionsState.value
        }
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        fakePlacesClient.findPredictionsCalls.awaitItem()
        assertThat(stateBeforeFetch).isEqualTo(InlinePredictionsState.Loading)
    }

    @Test
    fun `subsequent fetch keeps Results instead of Loading`() = runScenario {
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "123"
        advanceTimeBy(500)
        fakePlacesClient.findPredictionsCalls.awaitItem()

        var stateDuringRefetch: InlinePredictionsState? = null
        fakePlacesClient.onBeforeFindPredictions = {
            stateDuringRefetch = delegate.inlinePredictionsState.value
        }

        queryFlow.value = "1234"
        advanceTimeBy(500)
        fakePlacesClient.findPredictionsCalls.awaitItem()

        assertThat(stateDuringRefetch).isInstanceOf<InlinePredictionsState.Results>()
    }

    @Test
    fun `onPredictionSelected fetches place and emits OnExpandForm event`() = runScenario {
        fakePlacesClient.fetchPlaceResult = Result.success(
            Address(
                line1 = "123 Main Street",
                city = "San Francisco",
                state = "CA",
                country = "US",
                postalCode = "94105",
            )
        )

        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        assertThat(fakePlacesClient.fetchPlaceCalls.awaitItem().placeId)
            .isEqualTo("place_1")
        fakePlacesClient.resetSessionCalls.awaitItem()
        val event = eventCalls.awaitItem()
        assertThat(event)
            .isInstanceOf<AutocompleteAddressInteractor.Event.OnExpandForm>()
        val values =
            (event as AutocompleteAddressInteractor.Event.OnExpandForm).values
        assertThat(values?.get(IdentifierSpec.Line1)).isEqualTo("123 Main Street")
        assertThat(values?.get(IdentifierSpec.City)).isEqualTo("San Francisco")
        assertThat(values?.get(IdentifierSpec.State)).isEqualTo("CA")
        assertThat(values?.get(IdentifierSpec.Country)).isEqualTo("US")
        assertThat(values?.get(IdentifierSpec.PostalCode)).isEqualTo("94105")
    }

    @Test
    fun `onPredictionSelected resets state to Idle`() = runScenario {
        fakePlacesClient.fetchPlaceResult = Result.success(Address())

        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        fakePlacesClient.fetchPlaceCalls.awaitItem()
        fakePlacesClient.resetSessionCalls.awaitItem()
        eventCalls.awaitItem()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
    }

    @Test
    fun `onPredictionSelected with failed fetch shows empty results without emitting event`() =
        runScenario {
            fakePlacesClient.fetchPlaceResult =
                Result.failure(RuntimeException("Network error"))

            delegate.onPredictionSelected("place_1")
            advanceTimeBy(100)

            fakePlacesClient.fetchPlaceCalls.awaitItem()
            fakePlacesClient.resetSessionCalls.awaitItem()
            assertThat(delegate.inlinePredictionsState.value)
                .isEqualTo(InlinePredictionsState.Results(query = "", predictions = emptyList()))
        }

    @Test
    fun `onFocusLost does not cancel an in-flight prediction selection`() = runScenario {
        val fetchGate = CompletableDeferred<Unit>()
        fakePlacesClient.fetchPlaceResult = Result.success(
            Address(line1 = "123 Main Street", country = "US")
        )
        fakePlacesClient.onBeforeFetchPlace = { fetchGate.await() }

        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        delegate.onFocusLost()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)

        // Release the gate — selection must still complete despite focus loss
        fetchGate.complete(Unit)
        advanceTimeBy(100)

        assertThat(fakePlacesClient.fetchPlaceCalls.awaitItem().placeId).isEqualTo("place_1")
        fakePlacesClient.resetSessionCalls.awaitItem()
        eventCalls.awaitItem()
    }

    @Test
    fun `onDismissed cancels an in-flight prediction selection`() = runScenario {
        val fetchGate = CompletableDeferred<Unit>()
        fakePlacesClient.fetchPlaceResult = Result.success(
            Address(line1 = "123 Main Street", country = "US")
        )
        fakePlacesClient.onBeforeFetchPlace = { fetchGate.await() }

        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        delegate.onDismissed()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)

        // Release the gate — result must be discarded (job was already cancelled)
        fetchGate.complete(Unit)
        advanceTimeBy(100)

        assertThat(fakePlacesClient.fetchPlaceCalls.awaitItem().placeId).isEqualTo("place_1")
        fakePlacesClient.resetSessionCalls.awaitItem()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
        // No event — selection was cancelled before result was handled
    }

    @Test
    fun `onPredictionSelected deactivates — typing afterward does not trigger fetch`() =
        runScenario {
            fakePlacesClient.fetchPlaceResult = Result.success(
                Address(line1 = "123 Main Street", country = "US")
            )
            delegate.observeQueryChanges(queryFlow, countryFlow)

            delegate.onPredictionSelected("place_1")
            advanceTimeBy(100)

            fakePlacesClient.fetchPlaceCalls.awaitItem()
            fakePlacesClient.resetSessionCalls.awaitItem()
            eventCalls.awaitItem()

            // After selection isActive = false; typing must not trigger a fetch.
            queryFlow.value = "123 Main Street"
            advanceTimeBy(500)

            assertThat(delegate.inlinePredictionsState.value)
                .isEqualTo(InlinePredictionsState.Idle)
        }

    @Test
    fun `onSearchActivated after selection re-activates fetch`() =
        runScenario {
            fakePlacesClient.fetchPlaceResult = Result.success(
                Address(line1 = "123 Main Street", country = "US")
            )
            fakePlacesClient.findPredictionsResult = Result.success(
                FindAutocompletePredictionsResponse(emptyList())
            )
            delegate.observeQueryChanges(queryFlow, countryFlow)

            delegate.onPredictionSelected("place_1")
            advanceTimeBy(100)

            fakePlacesClient.fetchPlaceCalls.awaitItem()
            fakePlacesClient.resetSessionCalls.awaitItem()
            eventCalls.awaitItem()

            // Tapping the magnifying glass re-activates the search.
            queryFlow.value = "123 Main Street"
            delegate.onSearchActivated()
            advanceTimeBy(100)
            fakePlacesClient.findPredictionsCalls.awaitItem()

            assertThat(delegate.inlinePredictionsState.value)
                .isInstanceOf<InlinePredictionsState.Results>()
        }

    @Test
    fun `onFocusLost preserves suppression sentinel after selection`() = runScenario {
        fakePlacesClient.fetchPlaceResult = Result.success(
            Address(line1 = "123 Main Street", country = "US")
        )
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)

        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        fakePlacesClient.fetchPlaceCalls.awaitItem()
        fakePlacesClient.resetSessionCalls.awaitItem()
        eventCalls.awaitItem()

        // Focus shifts away (e.g. form fills and city field gains focus)
        delegate.onFocusLost()

        // User taps back into the line1 field — sentinel must survive the focus cycle
        delegate.onFocusGained()

        // The filled address query arrives — must be suppressed by the sentinel
        queryFlow.value = "123 Main Street"
        advanceTimeBy(500)

        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
        // No findPredictions call — verified by ensureAllEventsConsumed() in runScenario teardown
    }

    @Test
    fun `onFocusLost before debounce fires prevents stale fetch`() = runScenario {
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)

        queryFlow.value = "123 Main"
        // Focus lost before the debounce fires — cancels observeJob
        delegate.onFocusLost()
        advanceTimeBy(500)

        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
        // No findPredictions call — verified by ensureAllEventsConsumed
    }

    @Test
    fun `onFocusGained re-enables predictions after focus loss`() = runScenario {
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "123 Main"
        delegate.onFocusLost()
        advanceTimeBy(500)

        // Re-focus re-enables observation; isActive remains true
        delegate.onFocusGained()
        queryFlow.value = "123 Main S"
        advanceTimeBy(500)

        val call = fakePlacesClient.findPredictionsCalls.awaitItem()
        assertThat(call.query).isEqualTo("123 Main S")
    }

    @Test
    fun `onDismissed after selection allows re-activation with same query`() =
        runScenario {
            fakePlacesClient.fetchPlaceResult = Result.success(
                Address(line1 = "123 Main Street", country = "US")
            )
            fakePlacesClient.findPredictionsResult = Result.success(
                FindAutocompletePredictionsResponse(emptyList())
            )
            delegate.observeQueryChanges(queryFlow, countryFlow)

            delegate.onPredictionSelected("place_1")
            advanceTimeBy(100)

            fakePlacesClient.fetchPlaceCalls.awaitItem()
            fakePlacesClient.resetSessionCalls.awaitItem()
            eventCalls.awaitItem()

            // After selection, isActive=false. Dismiss then re-activate.
            queryFlow.value = "123 Main Street"
            delegate.onDismissed()
            delegate.onSearchActivated()
            advanceTimeBy(100)

            fakePlacesClient.findPredictionsCalls.awaitItem()
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
        delegate.onSearchActivated()
        queryFlow.value = "123 Main"
        advanceTimeBy(500)
        fakePlacesClient.findPredictionsCalls.awaitItem()
        assertThat(delegate.inlinePredictionsState.value)
            .isInstanceOf<InlinePredictionsState.Results>()

        delegate.onDismissed()

        assertThat(delegate.inlinePredictionsState.value)
            .isEqualTo(InlinePredictionsState.Idle)
    }

    @Test
    fun `while active, country change triggers re-evaluation`() = runScenario {
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)
        queryFlow.value = "123 Main"
        delegate.onSearchActivated()
        advanceTimeBy(100)
        fakePlacesClient.findPredictionsCalls.awaitItem()

        countryFlow.value = "CA"
        advanceTimeBy(500)
        val call = fakePlacesClient.findPredictionsCalls.awaitItem()
        assertThat(call.country).isEqualTo("CA")
    }

    @Test
    fun `country switches back to supported after focus loss re-emits OnValues`() = runScenario(
        autocompleteCountries = setOf("US")
    ) {
        delegate.observeQueryChanges(queryFlow, countryFlow)

        // Switching to an unsupported country expands the form.
        countryFlow.value = "CA"
        advanceTimeBy(500)
        assertThat(eventCalls.awaitItem()).isEqualTo(
            AutocompleteAddressInteractor.Event.OnValues(mapOf(IdentifierSpec.Country to "CA"))
        )

        // Expanding removes the inline field from composition, so focus is lost. The country
        // observation must survive this so the form can revert when a supported country returns.
        delegate.onFocusLost()

        // Switching back to a supported country must re-emit so the form returns to inline mode.
        countryFlow.value = "US"
        advanceTimeBy(500)
        assertThat(eventCalls.awaitItem()).isEqualTo(
            AutocompleteAddressInteractor.Event.OnValues(mapOf(IdentifierSpec.Country to "US"))
        )
    }

    @Test
    fun `calling observeQueryChanges again cancels previous observation`() =
        runScenario {
            fakePlacesClient.findPredictionsResult = Result.success(
                FindAutocompletePredictionsResponse(emptyList())
            )
            val secondQueryFlow = MutableStateFlow("")
            val secondCountryFlow = MutableStateFlow<String?>("US")

            delegate.observeQueryChanges(queryFlow, countryFlow)
            delegate.observeQueryChanges(secondQueryFlow, secondCountryFlow)
            delegate.onSearchActivated()

            queryFlow.value = "first query"
            advanceTimeBy(500)

            secondQueryFlow.value = "second query"
            advanceTimeBy(500)
            val call = fakePlacesClient.findPredictionsCalls.awaitItem()
            assertThat(call.query).isEqualTo("second query")
        }

    @Test
    fun `dispose cancels query observation`() = runScenario {
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        delegate.dispose()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        fakePlacesClient.findPredictionsCalls.expectNoEvents()
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
        delegate.onSearchActivated()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        val call = fakePlacesClient.findPredictionsCalls.awaitItem()
        assertThat(call.country).isEqualTo("")
    }

    @Test
    fun `prediction failure keeps dropdown open with empty results`() = runScenario(
        shouldUseStripeHostedAutocomplete = true,
    ) {
        fakePlacesClient.findPredictionsResult = Result.failure(
            RuntimeException("Hosted proxy unavailable")
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        val call = fakePlacesClient.findPredictionsCalls.awaitItem()
        assertThat(call.query).isEqualTo("123 Main")
        assertThat(call.country).isEqualTo("US")
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(
            InlinePredictionsState.Results(query = "123 Main", predictions = emptyList())
        )
        eventCalls.expectNoEvents()
    }

    @Test
    fun `refetch failure keeps dropdown open with empty results`() = runScenario(
        shouldUseStripeHostedAutocomplete = true,
    ) {
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "123"
        advanceTimeBy(500)
        fakePlacesClient.findPredictionsCalls.awaitItem()
        assertThat(delegate.inlinePredictionsState.value)
            .isInstanceOf<InlinePredictionsState.Results>()

        fakePlacesClient.findPredictionsResult = Result.failure(RuntimeException("Network error"))

        queryFlow.value = "1234"
        advanceTimeBy(500)
        fakePlacesClient.findPredictionsCalls.awaitItem()

        assertThat(delegate.inlinePredictionsState.value).isEqualTo(
            InlinePredictionsState.Results(query = "1234", predictions = emptyList())
        )
        eventCalls.expectNoEvents()
    }

    @Test
    fun `stripe-hosted config expands form on place selection failure`() = runScenario(
        shouldUseStripeHostedAutocomplete = true,
    ) {
        fakePlacesClient.fetchPlaceResult = Result.failure(
            RuntimeException("Hosted proxy unavailable")
        )
        delegate.onPredictionSelected("place-id-123")

        val call = fakePlacesClient.fetchPlaceCalls.awaitItem()
        assertThat(call.placeId).isEqualTo("place-id-123")
        fakePlacesClient.resetSessionCalls.awaitItem()
        assertThat(delegate.inlinePredictionsState.value)
            .isEqualTo(InlinePredictionsState.Results(query = "", predictions = emptyList()))
        assertThat(eventCalls.awaitItem())
            .isEqualTo(AutocompleteAddressInteractor.Event.OnExpandForm(values = null))
    }

    @Test
    fun `stripe-hosted fetchPlace failure after prior query includes query and country`() = runScenario(
        shouldUseStripeHostedAutocomplete = true,
    ) {
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(
                listOf(
                    AutocompletePrediction(
                        SpannableString("123 Main St"),
                        SpannableString("City, ST"),
                        "place-id-abc",
                    )
                )
            )
        )
        fakePlacesClient.fetchPlaceResult = Result.failure(
            RuntimeException("Hosted proxy unavailable")
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        fakePlacesClient.findPredictionsCalls.awaitItem()
        delegate.onPredictionSelected("place-id-abc")

        fakePlacesClient.fetchPlaceCalls.awaitItem()
        fakePlacesClient.resetSessionCalls.awaitItem()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(
            InlinePredictionsState.Results(query = "123 Main", predictions = emptyList())
        )
        assertThat(eventCalls.awaitItem()).isEqualTo(
            AutocompleteAddressInteractor.Event.OnExpandForm(
                values = mapOf(
                    IdentifierSpec.Line1 to "123 Main",
                    IdentifierSpec.Country to "US",
                )
            )
        )
    }

    @Test
    fun `stripe-hosted fetchPlace failure expands with edits made while fetch in flight`() = runScenario(
        shouldUseStripeHostedAutocomplete = true,
    ) {
        val fetchGate = CompletableDeferred<Unit>()
        fakePlacesClient.fetchPlaceResult = Result.failure(
            RuntimeException("Hosted proxy unavailable")
        )
        fakePlacesClient.onBeforeFetchPlace = { fetchGate.await() }
        delegate.observeQueryChanges(queryFlow, countryFlow)
        queryFlow.value = "123 Main"

        delegate.onPredictionSelected("place-id-abc")
        advanceTimeBy(100)

        // User keeps editing the field while fetchPlace is still in flight.
        queryFlow.value = "456 Oak Ave"

        // Release the gate — the fallback expansion must use the edited query, not the stale one.
        fetchGate.complete(Unit)
        advanceTimeBy(100)

        fakePlacesClient.fetchPlaceCalls.awaitItem()
        fakePlacesClient.resetSessionCalls.awaitItem()
        assertThat(eventCalls.awaitItem()).isEqualTo(
            AutocompleteAddressInteractor.Event.OnExpandForm(
                values = mapOf(
                    IdentifierSpec.Line1 to "456 Oak Ave",
                    IdentifierSpec.Country to "US",
                )
            )
        )
    }

    @Test
    fun `prediction failure with null country keeps dropdown open with empty results`() = runScenario(
        shouldUseStripeHostedAutocomplete = true,
    ) {
        fakePlacesClient.findPredictionsResult = Result.failure(
            RuntimeException("Hosted proxy unavailable")
        )
        countryFlow.value = null
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        fakePlacesClient.findPredictionsCalls.awaitItem()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(
            InlinePredictionsState.Results(query = "123 Main", predictions = emptyList())
        )
        eventCalls.expectNoEvents()
    }

    @Test
    fun `onSearchActivated triggers immediate fetch bypassing debounce`() =
        runScenario {
            fakePlacesClient.findPredictionsResult = Result.success(
                FindAutocompletePredictionsResponse(emptyList())
            )
            delegate.observeQueryChanges(queryFlow, countryFlow)
            queryFlow.value = "123 Main St"

            delegate.onSearchActivated()
            advanceTimeBy(100)

            val call = fakePlacesClient.findPredictionsCalls.awaitItem()
            assertThat(call.query).isEqualTo("123 Main St")
        }

    @Test
    fun `onSearchActivated does not double-fetch when debounce catches up`() =
        runScenario {
            fakePlacesClient.findPredictionsResult = Result.success(
                FindAutocompletePredictionsResponse(emptyList())
            )
            delegate.observeQueryChanges(queryFlow, countryFlow)
            queryFlow.value = "123 Main St"

            delegate.onSearchActivated()
            // Advance past the debounce window: the pending debounced emission for the
            // just-typed query must not fire a second request for the same query.
            advanceTimeBy(500)

            val call = fakePlacesClient.findPredictionsCalls.awaitItem()
            assertThat(call.query).isEqualTo("123 Main St")
            fakePlacesClient.findPredictionsCalls.expectNoEvents()
        }

    @Test
    fun `onSearchActivated with short query stays Idle but activates for typing`() =
        runScenario {
            fakePlacesClient.findPredictionsResult = Result.success(
                FindAutocompletePredictionsResponse(emptyList())
            )
            delegate.observeQueryChanges(queryFlow, countryFlow)

            // query = "" — too short for immediate fetch, but activates for subsequent typing
            delegate.onSearchActivated()

            queryFlow.value = "123 Main"
            advanceTimeBy(500)

            val call = fakePlacesClient.findPredictionsCalls.awaitItem()
            assertThat(call.query).isEqualTo("123 Main")
        }

    @Test
    fun `after search-activated dismiss, country change does not auto-trigger`() =
        runScenario {
            fakePlacesClient.findPredictionsResult = Result.success(
                FindAutocompletePredictionsResponse(emptyList())
            )
            delegate.observeQueryChanges(queryFlow, countryFlow)
            queryFlow.value = "123 Main"
            delegate.onSearchActivated()
            advanceTimeBy(100)
            fakePlacesClient.findPredictionsCalls.awaitItem()

            delegate.onDismissed()

            countryFlow.value = "CA"
            advanceTimeBy(500)

            assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
            fakePlacesClient.findPredictionsCalls.expectNoEvents()
        }

    @Test
    fun `expandFormFromInline emits OnExpandForm with null values when query is empty`() = runScenario {
        delegate.expandFormFromInline()

        assertThat(eventCalls.awaitItem())
            .isEqualTo(AutocompleteAddressInteractor.Event.OnExpandForm(values = null))
    }

    @Test
    fun `expandFormFromInline pre-fills Line1 from current query before debounce completes`() = runScenario {
        delegate.observeQueryChanges(queryFlow, countryFlow)

        queryFlow.value = "123 Main St"
        delegate.expandFormFromInline()

        assertThat(eventCalls.awaitItem()).isEqualTo(
            AutocompleteAddressInteractor.Event.OnExpandForm(
                values = mapOf(
                    IdentifierSpec.Line1 to "123 Main St",
                    IdentifierSpec.Country to "US",
                )
            )
        )
    }

    @Test
    fun `fetchPlace failure deactivates - re-activation after failure allows fetch`() = runScenario {
        fakePlacesClient.fetchPlaceResult = Result.failure(RuntimeException("network error"))
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)

        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        fakePlacesClient.fetchPlaceCalls.awaitItem()
        fakePlacesClient.resetSessionCalls.awaitItem()

        // After failure, isActive=false. Re-activate and verify fetch works.
        queryFlow.value = "123 Main Street"
        delegate.onSearchActivated()
        advanceTimeBy(100)

        fakePlacesClient.findPredictionsCalls.awaitItem()
    }

    @Test
    fun `autocompleteFilledAddress tracks selected prediction address`() = runScenario {
        val fetchedAddress = Address(
            line1 = "123 Main Street",
            city = "San Francisco",
            state = "CA",
            postalCode = "94105",
            country = "US",
        )
        fakePlacesClient.fetchPlaceResult = Result.success(fetchedAddress)
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)

        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        fakePlacesClient.fetchPlaceCalls.awaitItem()
        fakePlacesClient.resetSessionCalls.awaitItem()
        eventCalls.awaitItem()

        assertThat(delegate.autocompleteFilledAddress).isEqualTo(fetchedAddress)
    }

    @Test
    fun `autocompleteFilledAddress remains null after failed fetch`() = runScenario {
        fakePlacesClient.fetchPlaceResult = Result.failure(RuntimeException("network error"))
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)

        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        fakePlacesClient.fetchPlaceCalls.awaitItem()
        fakePlacesClient.resetSessionCalls.awaitItem()

        assertThat(delegate.autocompleteFilledAddress).isNull()
    }

    @Test
    fun `onDismissed while find-predictions fetch is in-flight prevents stale results`() = runScenario {
        val fetchGate = CompletableDeferred<Unit>()
        fakePlacesClient.onBeforeFindPredictions = { fetchGate.await() }
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        // Fetch is suspended at the gate — state should be Loading
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Loading)

        // Dismiss while fetch is in-flight
        delegate.onDismissed()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)

        // Release the gate — the result must be discarded, state stays Idle
        fetchGate.complete(Unit)
        advanceTimeBy(100)

        fakePlacesClient.findPredictionsCalls.awaitItem()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
    }

    @Test
    fun `query change during in-flight fetch discards stale result`() = runScenario {
        val fetchGate = CompletableDeferred<Unit>()
        fakePlacesClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(
                listOf(
                    AutocompletePrediction(
                        SpannableString("123 Main St"),
                        SpannableString("City, ST"),
                        "place_1",
                    )
                )
            )
        )
        fakePlacesClient.onBeforeFindPredictions = { fetchGate.await() }
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        // Fetch is suspended at the gate
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Loading)

        // User types more — query changes while fetch is in-flight
        queryFlow.value = "123 Main St"

        // Release the gate — stale result for "123 Main" must be discarded
        fetchGate.complete(Unit)
        advanceTimeBy(100)

        fakePlacesClient.findPredictionsCalls.awaitItem()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Loading)

        // The debounced fetch for the new query should fire
        fakePlacesClient.onBeforeFindPredictions = {}
        advanceTimeBy(500)
        val call = fakePlacesClient.findPredictionsCalls.awaitItem()
        assertThat(call.query).isEqualTo("123 Main St")
    }

    @Test
    fun `expanded mode - typing does not trigger predictions after expandFormFromInline`() =
        runScenario {
            fakePlacesClient.findPredictionsResult = Result.success(
                FindAutocompletePredictionsResponse(emptyList())
            )
            delegate.observeQueryChanges(queryFlow, countryFlow)

            delegate.expandFormFromInline()
            eventCalls.awaitItem()

            queryFlow.value = "123 Main"
            advanceTimeBy(500)

            assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
            fakePlacesClient.findPredictionsCalls.expectNoEvents()
        }

    @Test
    fun `expanded mode - search enables autocomplete, dismiss disables it`() =
        runScenario {
            fakePlacesClient.findPredictionsResult = Result.success(
                FindAutocompletePredictionsResponse(emptyList())
            )
            delegate.observeQueryChanges(queryFlow, countryFlow)

            delegate.expandFormFromInline()
            eventCalls.awaitItem()

            queryFlow.value = "123 Main"
            delegate.onSearchActivated()
            advanceTimeBy(100)
            fakePlacesClient.findPredictionsCalls.awaitItem()

            // Typing while search is active continues to trigger predictions
            queryFlow.value = "123 Main St"
            advanceTimeBy(500)
            val call = fakePlacesClient.findPredictionsCalls.awaitItem()
            assertThat(call.query).isEqualTo("123 Main St")

            // Dismiss (X button) deactivates — typing no longer triggers
            delegate.onDismissed()
            queryFlow.value = "123 Main Street"
            advanceTimeBy(500)
            fakePlacesClient.findPredictionsCalls.expectNoEvents()

            // Must click search again to re-enable
            delegate.onSearchActivated()
            advanceTimeBy(100)
            fakePlacesClient.findPredictionsCalls.awaitItem()
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun runScenario(
        autocompleteCountries: Set<String> = emptySet(),
        shouldUseStripeHostedAutocomplete: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        val fakePlaces = FakePlacesClientProxy(
            findPredictionsResult = Result.success(FindAutocompletePredictionsResponse(emptyList())),
            fetchPlaceResult = Result.success(Address()),
        )
        val eventCalls = Turbine<AutocompleteAddressInteractor.Event>()
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "test_key",
            autocompleteCountries = autocompleteCountries,
            isPlacesAvailable = true,
            isInlineAutocompleteEnabled = true,
            shouldUseStripeHostedAutocomplete = shouldUseStripeHostedAutocomplete,
        )
        val delegate = InlineAutocompleteController(
            placesClient = fakePlaces,
            config = config,
            coroutineScope = backgroundScope,
            eventListenerProvider = { { event -> eventCalls.add(event) } },
        )

        Scenario(
            delegate = delegate,
            fakePlacesClient = fakePlaces,
            eventCalls = eventCalls,
            queryFlow = MutableStateFlow(""),
            countryFlow = MutableStateFlow("US"),
            testScope = this,
        ).apply { block() }

        fakePlaces.ensureAllEventsConsumed()
        eventCalls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val delegate: InlineAutocompleteController,
        val fakePlacesClient: FakePlacesClientProxy,
        val eventCalls: Turbine<AutocompleteAddressInteractor.Event>,
        val queryFlow: MutableStateFlow<String>,
        val countryFlow: MutableStateFlow<String?>,
        val testScope: TestScope,
    ) {
        fun advanceTimeBy(millis: Long) = testScope.advanceTimeBy(millis)
    }
}
