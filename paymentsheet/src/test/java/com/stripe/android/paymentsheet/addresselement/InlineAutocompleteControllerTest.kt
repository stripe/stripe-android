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
import com.stripe.android.uicore.elements.FormFieldId
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
        delegate.onFocusGained()

        queryFlow.value = "ab"
        advanceTimeBy(500)

        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
    }

    @Test
    fun `query at minimum chars triggers fetch when active`() = runScenario {
        fakeAutocompleteClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "abc"
        advanceTimeBy(500)

        val call = fakeAutocompleteClient.findPredictionsCalls.awaitItem()
        assertThat(call.query).isEqualTo("abc")
    }

    @Test
    fun `query with unsupported country stays Idle`() = runScenario(
        autocompleteCountries = setOf("US")
    ) {
        countryFlow.value = "CA"
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onFocusGained()

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
        fakeAutocompleteClient.findPredictionsCalls.expectNoEvents()
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
                mapOf(FormFieldId.Country to "CA")
            )
        )
    }

    @Test
    fun `empty autocompleteCountries allows all countries`() = runScenario(
        autocompleteCountries = emptySet()
    ) {
        fakeAutocompleteClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        countryFlow.value = "JP"
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "Tokyo"
        advanceTimeBy(500)

        fakeAutocompleteClient.findPredictionsCalls.awaitItem()
    }

    @Test
    fun `country check is case insensitive`() = runScenario(
        autocompleteCountries = setOf("US")
    ) {
        fakeAutocompleteClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        countryFlow.value = "us"
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        fakeAutocompleteClient.findPredictionsCalls.awaitItem()
    }

    @Test
    fun `debounces rapid query changes`() = runScenario {
        fakeAutocompleteClient.findPredictionsResult = Result.success(
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

        val call = fakeAutocompleteClient.findPredictionsCalls.awaitItem()
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
        fakeAutocompleteClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(predictions)
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "Main"
        advanceTimeBy(500)

        fakeAutocompleteClient.findPredictionsCalls.awaitItem()
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
        fakeAutocompleteClient.findPredictionsResult = Result.failure(RuntimeException("Network error"))
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        fakeAutocompleteClient.findPredictionsCalls.awaitItem()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(
            InlinePredictionsState.Results(query = "123 Main", predictions = emptyList())
        )
    }

    @Test
    fun `state transitions to Loading before Results`() = runScenario {
        fakeAutocompleteClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        var stateBeforeFetch: InlinePredictionsState? = null
        fakeAutocompleteClient.onBeforeFindPredictions = {
            stateBeforeFetch = delegate.inlinePredictionsState.value
        }
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        fakeAutocompleteClient.findPredictionsCalls.awaitItem()
        assertThat(stateBeforeFetch).isEqualTo(InlinePredictionsState.Loading)
    }

    @Test
    fun `subsequent fetch keeps Results instead of Loading`() = runScenario {
        fakeAutocompleteClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "123"
        advanceTimeBy(500)
        fakeAutocompleteClient.findPredictionsCalls.awaitItem()

        var stateDuringRefetch: InlinePredictionsState? = null
        fakeAutocompleteClient.onBeforeFindPredictions = {
            stateDuringRefetch = delegate.inlinePredictionsState.value
        }

        queryFlow.value = "1234"
        advanceTimeBy(500)
        fakeAutocompleteClient.findPredictionsCalls.awaitItem()

        assertThat(stateDuringRefetch).isInstanceOf<InlinePredictionsState.Results>()
    }

    @Test
    fun `onPredictionSelected fetches place and emits OnExpandForm event`() = runScenario {
        fakeAutocompleteClient.fetchPlaceResult = Result.success(
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

        assertThat(fakeAutocompleteClient.fetchPlaceCalls.awaitItem().placeId)
            .isEqualTo("place_1")
        fakeAutocompleteClient.resetSessionCalls.awaitItem()
        val event = eventCalls.awaitItem()
        assertThat(event)
            .isInstanceOf<AutocompleteAddressInteractor.Event.OnExpandForm>()
        val values =
            (event as AutocompleteAddressInteractor.Event.OnExpandForm).values
        assertThat(values?.get(FormFieldId.Line1)).isEqualTo("123 Main Street")
        assertThat(values?.get(FormFieldId.City)).isEqualTo("San Francisco")
        assertThat(values?.get(FormFieldId.State)).isEqualTo("CA")
        assertThat(values?.get(FormFieldId.Country)).isEqualTo("US")
        assertThat(values?.get(FormFieldId.PostalCode)).isEqualTo("94105")
    }

    @Test
    fun `onPredictionSelected resets state to Idle`() = runScenario {
        fakeAutocompleteClient.fetchPlaceResult = Result.success(Address())

        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        fakeAutocompleteClient.fetchPlaceCalls.awaitItem()
        fakeAutocompleteClient.resetSessionCalls.awaitItem()
        eventCalls.awaitItem()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
    }

    @Test
    fun `onPredictionSelected with failed fetch shows empty results without emitting event`() =
        runScenario {
            fakeAutocompleteClient.fetchPlaceResult =
                Result.failure(RuntimeException("Network error"))

            delegate.onPredictionSelected("place_1")
            advanceTimeBy(100)

            fakeAutocompleteClient.fetchPlaceCalls.awaitItem()
            fakeAutocompleteClient.resetSessionCalls.awaitItem()
            assertThat(delegate.inlinePredictionsState.value)
                .isEqualTo(InlinePredictionsState.Results(query = "", predictions = emptyList()))
        }

    @Test
    fun `onFocusLost does not cancel an in-flight prediction selection`() = runScenario {
        val fetchGate = CompletableDeferred<Unit>()
        fakeAutocompleteClient.fetchPlaceResult = Result.success(
            Address(line1 = "123 Main Street", country = "US")
        )
        fakeAutocompleteClient.onBeforeFetchPlace = { fetchGate.await() }

        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        delegate.onFocusLost()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)

        // Release the gate — selection must still complete despite focus loss
        fetchGate.complete(Unit)
        advanceTimeBy(100)

        assertThat(fakeAutocompleteClient.fetchPlaceCalls.awaitItem().placeId).isEqualTo("place_1")
        fakeAutocompleteClient.resetSessionCalls.awaitItem()
        eventCalls.awaitItem()
    }

    @Test
    fun `onDismissed cancels an in-flight prediction selection`() = runScenario {
        val fetchGate = CompletableDeferred<Unit>()
        fakeAutocompleteClient.fetchPlaceResult = Result.success(
            Address(line1 = "123 Main Street", country = "US")
        )
        fakeAutocompleteClient.onBeforeFetchPlace = { fetchGate.await() }

        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        delegate.onDismissed()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)

        // Release the gate — result must be discarded (job was already cancelled)
        fetchGate.complete(Unit)
        advanceTimeBy(100)

        assertThat(fakeAutocompleteClient.fetchPlaceCalls.awaitItem().placeId).isEqualTo("place_1")
        fakeAutocompleteClient.resetSessionCalls.awaitItem()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
        // No event — selection was cancelled before result was handled
    }

    @Test
    fun `onPredictionSelected deactivates — typing afterward does not trigger fetch`() =
        runScenario {
            fakeAutocompleteClient.fetchPlaceResult = Result.success(
                Address(line1 = "123 Main Street", country = "US")
            )
            delegate.observeQueryChanges(queryFlow, countryFlow)
            delegate.onFocusGained()

            delegate.onPredictionSelected("place_1")
            advanceTimeBy(100)

            fakeAutocompleteClient.fetchPlaceCalls.awaitItem()
            fakeAutocompleteClient.resetSessionCalls.awaitItem()
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
            fakeAutocompleteClient.fetchPlaceResult = Result.success(
                Address(line1 = "123 Main Street", country = "US")
            )
            fakeAutocompleteClient.findPredictionsResult = Result.success(
                FindAutocompletePredictionsResponse(emptyList())
            )
            delegate.observeQueryChanges(queryFlow, countryFlow)

            delegate.onPredictionSelected("place_1")
            advanceTimeBy(100)

            fakeAutocompleteClient.fetchPlaceCalls.awaitItem()
            fakeAutocompleteClient.resetSessionCalls.awaitItem()
            eventCalls.awaitItem()

            // Tapping the magnifying glass re-activates the search.
            queryFlow.value = "123 Main Street"
            delegate.onSearchActivated()
            advanceTimeBy(100)
            fakeAutocompleteClient.findPredictionsCalls.awaitItem()

            assertThat(delegate.inlinePredictionsState.value)
                .isInstanceOf<InlinePredictionsState.Results>()
        }

    @Test
    fun `onFocusLost preserves suppression sentinel after selection`() = runScenario {
        fakeAutocompleteClient.fetchPlaceResult = Result.success(
            Address(line1 = "123 Main Street", country = "US")
        )
        fakeAutocompleteClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)

        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        fakeAutocompleteClient.fetchPlaceCalls.awaitItem()
        fakeAutocompleteClient.resetSessionCalls.awaitItem()
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
        fakeAutocompleteClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onFocusGained()

        queryFlow.value = "123 Main"
        // Focus lost before the debounce fires pauses predictions
        delegate.onFocusLost()
        advanceTimeBy(500)

        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
        // No findPredictions call — verified by ensureAllEventsConsumed
    }

    @Test
    fun `onFocusGained re-enables predictions after focus loss`() = runScenario {
        fakeAutocompleteClient.findPredictionsResult = Result.success(
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

        val call = fakeAutocompleteClient.findPredictionsCalls.awaitItem()
        assertThat(call.query).isEqualTo("123 Main S")
    }

    @Test
    fun `prefilled query does not fetch before focus or search`() = runScenario {
        queryFlow.value = "123 Coffee Street"
        delegate.observeQueryChanges(queryFlow, countryFlow)
        advanceTimeBy(500)

        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
        fakeAutocompleteClient.findPredictionsCalls.expectNoEvents()
    }

    @Test
    fun `onFocusGained does not fetch prefilled query until it changes`() = runScenario {
        queryFlow.value = "123 Coffee Street"
        delegate.observeQueryChanges(queryFlow, countryFlow)
        advanceTimeBy(500)

        delegate.onFocusGained()
        queryFlow.value = "123 Coffee Street A"
        advanceTimeBy(500)

        assertThat(fakeAutocompleteClient.findPredictionsCalls.awaitItem().query)
            .isEqualTo("123 Coffee Street A")
    }

    @Test
    fun `onSearchActivated fetches prefilled query without focus`() = runScenario {
        queryFlow.value = "123 Coffee Street"
        delegate.observeQueryChanges(queryFlow, countryFlow)
        advanceTimeBy(500)

        delegate.onSearchActivated()

        assertThat(fakeAutocompleteClient.findPredictionsCalls.awaitItem().query)
            .isEqualTo("123 Coffee Street")
    }

    @Test
    fun `onDismissed after selection allows re-activation with same query`() =
        runScenario {
            fakeAutocompleteClient.fetchPlaceResult = Result.success(
                Address(line1 = "123 Main Street", country = "US")
            )
            fakeAutocompleteClient.findPredictionsResult = Result.success(
                FindAutocompletePredictionsResponse(emptyList())
            )
            delegate.observeQueryChanges(queryFlow, countryFlow)

            delegate.onPredictionSelected("place_1")
            advanceTimeBy(100)

            fakeAutocompleteClient.fetchPlaceCalls.awaitItem()
            fakeAutocompleteClient.resetSessionCalls.awaitItem()
            eventCalls.awaitItem()

            // After selection, isActive=false. Dismiss then re-activate.
            queryFlow.value = "123 Main Street"
            delegate.onDismissed()
            delegate.onSearchActivated()
            advanceTimeBy(100)

            fakeAutocompleteClient.findPredictionsCalls.awaitItem()
        }

    @Test
    fun `onDismissed resets state to Idle`() = runScenario {
        fakeAutocompleteClient.findPredictionsResult = Result.success(
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
        fakeAutocompleteClient.findPredictionsCalls.awaitItem()
        assertThat(delegate.inlinePredictionsState.value)
            .isInstanceOf<InlinePredictionsState.Results>()

        delegate.onDismissed()

        assertThat(delegate.inlinePredictionsState.value)
            .isEqualTo(InlinePredictionsState.Idle)
    }

    @Test
    fun `while active, country change triggers re-evaluation`() = runScenario {
        fakeAutocompleteClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)
        queryFlow.value = "123 Main"
        delegate.onSearchActivated()
        advanceTimeBy(100)
        fakeAutocompleteClient.findPredictionsCalls.awaitItem()

        countryFlow.value = "CA"
        advanceTimeBy(500)
        val call = fakeAutocompleteClient.findPredictionsCalls.awaitItem()
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
            AutocompleteAddressInteractor.Event.OnValues(mapOf(FormFieldId.Country to "CA"))
        )

        // Expanding removes the inline field from composition, so focus is lost. The country
        // observation must survive this so the form can revert when a supported country returns.
        delegate.onFocusLost()

        // Switching back to a supported country must re-emit so the form returns to inline mode.
        countryFlow.value = "US"
        advanceTimeBy(500)
        assertThat(eventCalls.awaitItem()).isEqualTo(
            AutocompleteAddressInteractor.Event.OnValues(mapOf(FormFieldId.Country to "US"))
        )
    }

    @Test
    fun `calling observeQueryChanges again cancels previous observation`() =
        runScenario {
            fakeAutocompleteClient.findPredictionsResult = Result.success(
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
            val call = fakeAutocompleteClient.findPredictionsCalls.awaitItem()
            assertThat(call.query).isEqualTo("second query")
        }

    @Test
    fun `dispose cancels query observation`() = runScenario {
        fakeAutocompleteClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        delegate.dispose()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        fakeAutocompleteClient.findPredictionsCalls.expectNoEvents()
    }

    @Test
    fun `null country flow value defaults to empty string`() = runScenario(
        autocompleteCountries = emptySet()
    ) {
        fakeAutocompleteClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        countryFlow.value = null
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        val call = fakeAutocompleteClient.findPredictionsCalls.awaitItem()
        assertThat(call.country).isEqualTo("")
    }

    @Test
    fun `prediction failure keeps dropdown open with empty results`() = runScenario(
        shouldUseStripeHostedAutocomplete = true,
    ) {
        fakeAutocompleteClient.findPredictionsResult = Result.failure(
            RuntimeException("Hosted proxy unavailable")
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        val call = fakeAutocompleteClient.findPredictionsCalls.awaitItem()
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
        fakeAutocompleteClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "123"
        advanceTimeBy(500)
        fakeAutocompleteClient.findPredictionsCalls.awaitItem()
        assertThat(delegate.inlinePredictionsState.value)
            .isInstanceOf<InlinePredictionsState.Results>()

        fakeAutocompleteClient.findPredictionsResult = Result.failure(RuntimeException("Network error"))

        queryFlow.value = "1234"
        advanceTimeBy(500)
        fakeAutocompleteClient.findPredictionsCalls.awaitItem()

        assertThat(delegate.inlinePredictionsState.value).isEqualTo(
            InlinePredictionsState.Results(query = "1234", predictions = emptyList())
        )
        eventCalls.expectNoEvents()
    }

    @Test
    fun `stripe-hosted config expands form on place selection failure`() = runScenario(
        shouldUseStripeHostedAutocomplete = true,
    ) {
        fakeAutocompleteClient.fetchPlaceResult = Result.failure(
            RuntimeException("Hosted proxy unavailable")
        )
        delegate.onPredictionSelected("place-id-123")

        val call = fakeAutocompleteClient.fetchPlaceCalls.awaitItem()
        assertThat(call.placeId).isEqualTo("place-id-123")
        fakeAutocompleteClient.resetSessionCalls.awaitItem()
        assertThat(delegate.inlinePredictionsState.value)
            .isEqualTo(InlinePredictionsState.Results(query = "", predictions = emptyList()))
        assertThat(eventCalls.awaitItem())
            .isEqualTo(AutocompleteAddressInteractor.Event.OnExpandForm(values = null))
    }

    @Test
    fun `stripe-hosted fetchPlace failure after prior query includes query and country`() = runScenario(
        shouldUseStripeHostedAutocomplete = true,
    ) {
        fakeAutocompleteClient.findPredictionsResult = Result.success(
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
        fakeAutocompleteClient.fetchPlaceResult = Result.failure(
            RuntimeException("Hosted proxy unavailable")
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        fakeAutocompleteClient.findPredictionsCalls.awaitItem()
        delegate.onPredictionSelected("place-id-abc")

        fakeAutocompleteClient.fetchPlaceCalls.awaitItem()
        fakeAutocompleteClient.resetSessionCalls.awaitItem()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(
            InlinePredictionsState.Results(query = "123 Main", predictions = emptyList())
        )
        assertThat(eventCalls.awaitItem()).isEqualTo(
            AutocompleteAddressInteractor.Event.OnExpandForm(
                values = mapOf(
                    FormFieldId.Line1 to "123 Main",
                    FormFieldId.Country to "US",
                )
            )
        )
    }

    @Test
    fun `stripe-hosted fetchPlace failure expands with edits made while fetch in flight`() = runScenario(
        shouldUseStripeHostedAutocomplete = true,
    ) {
        val fetchGate = CompletableDeferred<Unit>()
        fakeAutocompleteClient.fetchPlaceResult = Result.failure(
            RuntimeException("Hosted proxy unavailable")
        )
        fakeAutocompleteClient.onBeforeFetchPlace = { fetchGate.await() }
        delegate.observeQueryChanges(queryFlow, countryFlow)
        queryFlow.value = "123 Main"

        delegate.onPredictionSelected("place-id-abc")
        advanceTimeBy(100)

        // User keeps editing the field while fetchPlace is still in flight.
        queryFlow.value = "456 Oak Ave"

        // Release the gate — the fallback expansion must use the edited query, not the stale one.
        fetchGate.complete(Unit)
        advanceTimeBy(100)

        fakeAutocompleteClient.fetchPlaceCalls.awaitItem()
        fakeAutocompleteClient.resetSessionCalls.awaitItem()
        assertThat(eventCalls.awaitItem()).isEqualTo(
            AutocompleteAddressInteractor.Event.OnExpandForm(
                values = mapOf(
                    FormFieldId.Line1 to "456 Oak Ave",
                    FormFieldId.Country to "US",
                )
            )
        )
    }

    @Test
    fun `prediction failure with null country keeps dropdown open with empty results`() = runScenario(
        shouldUseStripeHostedAutocomplete = true,
    ) {
        fakeAutocompleteClient.findPredictionsResult = Result.failure(
            RuntimeException("Hosted proxy unavailable")
        )
        countryFlow.value = null
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onSearchActivated()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        fakeAutocompleteClient.findPredictionsCalls.awaitItem()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(
            InlinePredictionsState.Results(query = "123 Main", predictions = emptyList())
        )
        eventCalls.expectNoEvents()
    }

    @Test
    fun `onSearchActivated triggers immediate fetch bypassing debounce`() =
        runScenario {
            fakeAutocompleteClient.findPredictionsResult = Result.success(
                FindAutocompletePredictionsResponse(emptyList())
            )
            delegate.observeQueryChanges(queryFlow, countryFlow)
            queryFlow.value = "123 Main St"

            delegate.onSearchActivated()
            advanceTimeBy(100)

            val call = fakeAutocompleteClient.findPredictionsCalls.awaitItem()
            assertThat(call.query).isEqualTo("123 Main St")
        }

    @Test
    fun `onSearchActivated fetches even when field is unfocused`() =
        runScenario {
            fakeAutocompleteClient.findPredictionsResult = Result.success(
                FindAutocompletePredictionsResponse(emptyList())
            )
            delegate.observeQueryChanges(queryFlow, countryFlow)
            delegate.onSearchActivated()
            queryFlow.value = "123 Main St"
            advanceTimeBy(500)
            fakeAutocompleteClient.findPredictionsCalls.awaitItem()

            delegate.onFocusLost()
            advanceTimeBy(100)

            delegate.onSearchActivated()
            advanceTimeBy(100)

            val call = fakeAutocompleteClient.findPredictionsCalls.awaitItem()
            assertThat(call.query).isEqualTo("123 Main St")
        }

    @Test
    fun `onSearchActivated does not double-fetch when debounce catches up`() =
        runScenario {
            fakeAutocompleteClient.findPredictionsResult = Result.success(
                FindAutocompletePredictionsResponse(emptyList())
            )
            delegate.observeQueryChanges(queryFlow, countryFlow)
            queryFlow.value = "123 Main St"

            delegate.onSearchActivated()
            // Advance past the debounce window: the pending debounced emission for the
            // just-typed query must not fire a second request for the same query.
            advanceTimeBy(500)

            val call = fakeAutocompleteClient.findPredictionsCalls.awaitItem()
            assertThat(call.query).isEqualTo("123 Main St")
            fakeAutocompleteClient.findPredictionsCalls.expectNoEvents()
        }

    @Test
    fun `onSearchActivated with short query stays Idle but activates for typing`() =
        runScenario {
            fakeAutocompleteClient.findPredictionsResult = Result.success(
                FindAutocompletePredictionsResponse(emptyList())
            )
            delegate.observeQueryChanges(queryFlow, countryFlow)

            // query = "" — too short for immediate fetch, but activates for subsequent typing
            delegate.onSearchActivated()

            queryFlow.value = "123 Main"
            advanceTimeBy(500)

            val call = fakeAutocompleteClient.findPredictionsCalls.awaitItem()
            assertThat(call.query).isEqualTo("123 Main")
        }

    @Test
    fun `after search-activated dismiss, country change does not auto-trigger`() =
        runScenario {
            fakeAutocompleteClient.findPredictionsResult = Result.success(
                FindAutocompletePredictionsResponse(emptyList())
            )
            delegate.observeQueryChanges(queryFlow, countryFlow)
            queryFlow.value = "123 Main"
            delegate.onSearchActivated()
            advanceTimeBy(100)
            fakeAutocompleteClient.findPredictionsCalls.awaitItem()

            delegate.onDismissed()

            countryFlow.value = "CA"
            advanceTimeBy(500)

            assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
            fakeAutocompleteClient.findPredictionsCalls.expectNoEvents()
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
                    FormFieldId.Line1 to "123 Main St",
                    FormFieldId.Country to "US",
                )
            )
        )
    }

    @Test
    fun `fetchPlace failure deactivates - re-activation after failure allows fetch`() = runScenario {
        fakeAutocompleteClient.fetchPlaceResult = Result.failure(RuntimeException("network error"))
        fakeAutocompleteClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)

        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        fakeAutocompleteClient.fetchPlaceCalls.awaitItem()
        fakeAutocompleteClient.resetSessionCalls.awaitItem()

        // After failure, isActive=false. Re-activate and verify fetch works.
        queryFlow.value = "123 Main Street"
        delegate.onSearchActivated()
        advanceTimeBy(100)

        fakeAutocompleteClient.findPredictionsCalls.awaitItem()
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
        fakeAutocompleteClient.fetchPlaceResult = Result.success(fetchedAddress)
        fakeAutocompleteClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)

        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        fakeAutocompleteClient.fetchPlaceCalls.awaitItem()
        fakeAutocompleteClient.resetSessionCalls.awaitItem()
        eventCalls.awaitItem()

        assertThat(delegate.autocompleteFilledAddress).isEqualTo(fetchedAddress)
    }

    @Test
    fun `autocompleteFilledAddress remains null after failed fetch`() = runScenario {
        fakeAutocompleteClient.fetchPlaceResult = Result.failure(RuntimeException("network error"))
        fakeAutocompleteClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(emptyList())
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)

        delegate.onPredictionSelected("place_1")
        advanceTimeBy(100)

        fakeAutocompleteClient.fetchPlaceCalls.awaitItem()
        fakeAutocompleteClient.resetSessionCalls.awaitItem()

        assertThat(delegate.autocompleteFilledAddress).isNull()
    }

    @Test
    fun `onDismissed while find-predictions fetch is in-flight prevents stale results`() = runScenario {
        val fetchGate = CompletableDeferred<Unit>()
        fakeAutocompleteClient.onBeforeFindPredictions = { fetchGate.await() }
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

        fakeAutocompleteClient.findPredictionsCalls.awaitItem()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
    }

    @Test
    fun `query change during in-flight fetch discards stale result`() = runScenario {
        val fetchGate = CompletableDeferred<Unit>()
        fakeAutocompleteClient.findPredictionsResult = Result.success(
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
        fakeAutocompleteClient.onBeforeFindPredictions = { fetchGate.await() }
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

        fakeAutocompleteClient.findPredictionsCalls.awaitItem()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Loading)

        // The debounced fetch for the new query should fire
        fakeAutocompleteClient.onBeforeFindPredictions = {}
        advanceTimeBy(500)
        val call = fakeAutocompleteClient.findPredictionsCalls.awaitItem()
        assertThat(call.query).isEqualTo("123 Main St")
    }

    @Test
    fun `editing away and back fetches predictions for the original query again`() = runScenario {
        val fetchGate = CompletableDeferred<Unit>()
        fakeAutocompleteClient.onBeforeFindPredictions = { fetchGate.await() }
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onFocusGained()

        queryFlow.value = "123 Main"
        advanceTimeBy(500)
        assertThat(fakeAutocompleteClient.findPredictionsCalls.awaitItem().query).isEqualTo("123 Main")

        // Edit away and back within the debounce window while the first fetch is still in flight.
        queryFlow.value = "123 Main S"
        fetchGate.complete(Unit)
        queryFlow.value = "123 Main"
        advanceTimeBy(500)

        assertThat(fakeAutocompleteClient.findPredictionsCalls.awaitItem().query).isEqualTo("123 Main")
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(
            InlinePredictionsState.Results(query = "123 Main", predictions = emptyList())
        )
    }

    @Test
    fun `prediction result arriving after place selection succeeds is discarded`() = runScenario {
        val fetchPlaceGate = CompletableDeferred<Unit>()
        val findPredictionsGate = CompletableDeferred<Unit>()
        // Matching line1 keeps the expanded form's query equal to the in-flight one, so only the
        // Idle check can discard the late result.
        fakeAutocompleteClient.fetchPlaceResult = Result.success(Address(line1 = "456 Oak Ave"))
        fakeAutocompleteClient.onBeforeFetchPlace = { fetchPlaceGate.await() }
        fakeAutocompleteClient.onBeforeFindPredictions = { findPredictionsGate.await() }
        delegate.observeQueryChanges(queryFlow, countryFlow)
        delegate.onFocusGained()
        queryFlow.value = "123 Main"

        delegate.onPredictionSelected("place_1")
        fakeAutocompleteClient.fetchPlaceCalls.awaitItem()

        // Typing during selection starts an independent prediction fetch.
        queryFlow.value = "456 Oak Ave"
        advanceTimeBy(500)
        assertThat(fakeAutocompleteClient.findPredictionsCalls.awaitItem().query).isEqualTo("456 Oak Ave")

        fetchPlaceGate.complete(Unit)
        fakeAutocompleteClient.resetSessionCalls.awaitItem()
        assertThat(eventCalls.awaitItem())
            .isInstanceOf<AutocompleteAddressInteractor.Event.OnExpandForm>()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)

        findPredictionsGate.complete(Unit)
        advanceTimeBy(100)

        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
    }

    @Test
    fun `search reopens predictions for the same query after dismissal`() = runScenario {
        fakeAutocompleteClient.findPredictionsResult = Result.success(
            FindAutocompletePredictionsResponse(
                listOf(
                    AutocompletePrediction(
                        SpannableString("123 Main St"),
                        SpannableString("San Francisco, CA"),
                        "place_1",
                    )
                )
            )
        )
        delegate.observeQueryChanges(queryFlow, countryFlow)
        queryFlow.value = "123 Main"

        delegate.onSearchActivated()
        assertThat(fakeAutocompleteClient.findPredictionsCalls.awaitItem().query).isEqualTo("123 Main")
        val state = delegate.inlinePredictionsState.value
        assertThat(state).isInstanceOf<InlinePredictionsState.Results>()
        val results = state as InlinePredictionsState.Results
        assertThat(results.query).isEqualTo("123 Main")
        assertThat(results.predictions.single().id).isEqualTo("place_1")

        delegate.onDismissed()
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)

        delegate.onSearchActivated()
        assertThat(fakeAutocompleteClient.findPredictionsCalls.awaitItem().query).isEqualTo("123 Main")
        assertThat(delegate.inlinePredictionsState.value).isEqualTo(results)
    }

    @Test
    fun `search cancels an in-flight place selection`() = runScenario {
        val fetchPlaceGate = CompletableDeferred<Unit>()
        fakeAutocompleteClient.onBeforeFetchPlace = { fetchPlaceGate.await() }
        delegate.observeQueryChanges(queryFlow, countryFlow)
        queryFlow.value = "123 Main"

        delegate.onPredictionSelected("place_1")
        fakeAutocompleteClient.fetchPlaceCalls.awaitItem()

        delegate.onSearchActivated()
        fakeAutocompleteClient.findPredictionsCalls.awaitItem()
        fakeAutocompleteClient.resetSessionCalls.awaitItem()

        fetchPlaceGate.complete(Unit)
        advanceTimeBy(100)

        eventCalls.expectNoEvents()
        assertThat(delegate.autocompleteFilledAddress).isNull()
    }

    @Test
    fun `expanded mode - typing does not trigger predictions after expandFormFromInline`() =
        runScenario {
            fakeAutocompleteClient.findPredictionsResult = Result.success(
                FindAutocompletePredictionsResponse(emptyList())
            )
            delegate.observeQueryChanges(queryFlow, countryFlow)
            delegate.onFocusGained()

            delegate.expandFormFromInline()
            eventCalls.awaitItem()

            queryFlow.value = "123 Main"
            advanceTimeBy(500)

            assertThat(delegate.inlinePredictionsState.value).isEqualTo(InlinePredictionsState.Idle)
            fakeAutocompleteClient.findPredictionsCalls.expectNoEvents()
        }

    @Test
    fun `expanded mode - search enables autocomplete, dismiss disables it`() =
        runScenario {
            fakeAutocompleteClient.findPredictionsResult = Result.success(
                FindAutocompletePredictionsResponse(emptyList())
            )
            delegate.observeQueryChanges(queryFlow, countryFlow)

            delegate.expandFormFromInline()
            eventCalls.awaitItem()

            queryFlow.value = "123 Main"
            delegate.onSearchActivated()
            advanceTimeBy(100)
            fakeAutocompleteClient.findPredictionsCalls.awaitItem()

            // Typing while search is active continues to trigger predictions
            queryFlow.value = "123 Main St"
            advanceTimeBy(500)
            val call = fakeAutocompleteClient.findPredictionsCalls.awaitItem()
            assertThat(call.query).isEqualTo("123 Main St")

            // Dismiss (X button) deactivates — typing no longer triggers
            delegate.onDismissed()
            queryFlow.value = "123 Main Street"
            advanceTimeBy(500)
            fakeAutocompleteClient.findPredictionsCalls.expectNoEvents()

            // Must click search again to re-enable
            delegate.onSearchActivated()
            advanceTimeBy(100)
            fakeAutocompleteClient.findPredictionsCalls.awaitItem()
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun runScenario(
        autocompleteCountries: Set<String> = emptySet(),
        shouldUseStripeHostedAutocomplete: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        val fakeAutocomplete = FakeAddressAutocompleteClient(
            findPredictionsResult = Result.success(FindAutocompletePredictionsResponse(emptyList())),
            fetchPlaceResult = Result.success(Address()),
        )
        val eventCalls = Turbine<AutocompleteAddressInteractor.Event>()
        val config = AutocompleteAddressInteractor.Config(
            autocompleteCountries = autocompleteCountries,
            shouldUseStripeHostedAutocomplete = shouldUseStripeHostedAutocomplete,
        )
        val delegate = InlineAutocompleteController(
            autocompleteClient = fakeAutocomplete,
            config = config,
            coroutineScope = backgroundScope,
            eventListenerProvider = { { event -> eventCalls.add(event) } },
        )

        Scenario(
            delegate = delegate,
            fakeAutocompleteClient = fakeAutocomplete,
            eventCalls = eventCalls,
            queryFlow = MutableStateFlow(""),
            countryFlow = MutableStateFlow("US"),
            testScope = this,
        ).apply { block() }

        fakeAutocomplete.ensureAllEventsConsumed()
        eventCalls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val delegate: InlineAutocompleteController,
        val fakeAutocompleteClient: FakeAddressAutocompleteClient,
        val eventCalls: Turbine<AutocompleteAddressInteractor.Event>,
        val queryFlow: MutableStateFlow<String>,
        val countryFlow: MutableStateFlow<String?>,
        val testScope: TestScope,
    ) {
        fun advanceTimeBy(millis: Long) = testScope.advanceTimeBy(millis)
    }
}
