package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Wrapper-specific behavior only — controller logic is tested in InlineAutocompleteControllerTest.
@RunWith(RobolectricTestRunner::class)
class BillingInlineAutocompleteAddressInteractorTest {

    @Test
    fun `onEnterManuallyFromInline emits OnExpandForm with null values when query is empty`() = runScenario {
        interactor.onEnterManuallyFromInline()

        assertThat(eventCalls.awaitItem())
            .isEqualTo(AutocompleteAddressInteractor.Event.OnExpandForm(values = null))
    }

    @Test
    fun `onPredictionSelected forwards the resulting event to the registered listener`() = runScenario {
        // Address-mapping detail is covered by InlineAutocompleteControllerTest; here we only
        // verify the event the controller produces reaches the listener wired via register().
        fakeAutocompleteClient.fetchPlaceResult = Result.success(Address())

        interactor.onPredictionSelected("place_1")
        advanceTimeBy(100)

        assertThat(fakeAutocompleteClient.fetchPlaceCalls.awaitItem().placeId).isEqualTo("place_1")
        fakeAutocompleteClient.resetSessionCalls.awaitItem()
        assertThat(eventCalls.awaitItem())
            .isInstanceOf(AutocompleteAddressInteractor.Event.OnExpandForm::class.java)
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        val fakeAutocomplete = FakeAddressAutocompleteClient(
            findPredictionsResult = Result.success(FindAutocompletePredictionsResponse(emptyList())),
            fetchPlaceResult = Result.success(Address()),
        )
        val eventCalls = Turbine<AutocompleteAddressInteractor.Event>()
        val config = AutocompleteAddressInteractor.Config(
            autocompleteCountries = emptySet(),
        )
        val interactor = BillingInlineAutocompleteAddressInteractor(
            autocompleteClient = fakeAutocomplete,
            autocompleteConfig = config,
            coroutineScope = backgroundScope,
        )
        interactor.register { event -> eventCalls.add(event) }

        Scenario(
            interactor = interactor,
            fakeAutocompleteClient = fakeAutocomplete,
            eventCalls = eventCalls,
            testScope = this,
        ).apply { block() }

        fakeAutocomplete.ensureAllEventsConsumed()
        eventCalls.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val interactor: BillingInlineAutocompleteAddressInteractor,
        val fakeAutocompleteClient: FakeAddressAutocompleteClient,
        val eventCalls: Turbine<AutocompleteAddressInteractor.Event>,
        val testScope: TestScope,
    ) {
        fun advanceTimeBy(millis: Long) = testScope.advanceTimeBy(millis)
    }
}
