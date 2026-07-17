package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.Place
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
    fun `onAutocomplete is a no-op and emits no event`() = runScenario {
        interactor.onAutocomplete("US")

        eventCalls.expectNoEvents()
    }

    @Test
    fun `onEnterManuallyFromInline emits OnExpandForm with null values`() = runScenario {
        interactor.onEnterManuallyFromInline()

        assertThat(eventCalls.awaitItem())
            .isEqualTo(AutocompleteAddressInteractor.Event.OnExpandForm(values = null))
    }

    @Test
    fun `onPredictionSelected forwards the resulting event to the registered listener`() = runScenario {
        // Address-mapping detail is covered by InlineAutocompleteControllerTest; here we only
        // verify the event the controller produces reaches the listener wired via register().
        fakePlacesClient.fetchPlaceResult = Result.success(FetchPlaceResponse(Place(emptyList())))

        interactor.onPredictionSelected("place_1")
        advanceTimeBy(100)

        assertThat(fakePlacesClient.fetchPlaceCalls.awaitItem()).isEqualTo("place_1")
        assertThat(eventCalls.awaitItem())
            .isInstanceOf(AutocompleteAddressInteractor.Event.OnValues::class.java)
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        val fakePlaces = FakePlacesClientProxy()
        val eventCalls = Turbine<AutocompleteAddressInteractor.Event>()
        val config = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "test_key",
            autocompleteCountries = emptySet(),
            isPlacesAvailable = true,
            isInlineAutocompleteEnabled = true,
        )
        val interactor = BillingInlineAutocompleteAddressInteractor(
            placesClient = fakePlaces,
            autocompleteConfig = config,
            coroutineScope = backgroundScope,
            shouldUseAutocompleteProxyEndpoints = false,
            stripeAutocompleteApiService = null,
        )
        interactor.register { event -> eventCalls.add(event) }

        Scenario(
            interactor = interactor,
            fakePlacesClient = fakePlaces,
            eventCalls = eventCalls,
            testScope = this,
        ).apply { block() }

        fakePlaces.ensureAllEventsConsumed()
        eventCalls.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val interactor: BillingInlineAutocompleteAddressInteractor,
        val fakePlacesClient: FakePlacesClientProxy,
        val eventCalls: Turbine<AutocompleteAddressInteractor.Event>,
        val testScope: TestScope,
    ) {
        fun advanceTimeBy(millis: Long) = testScope.advanceTimeBy(millis)
    }
}
