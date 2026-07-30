package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.paymentsheet.addresselement.analytics.FakeAddressLauncherEventReporter
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Locale

class StripeHostedPlacesClientProxyTest {

    @Test
    fun `findAutocompletePredictions maps result to FindAutocompletePredictionsResponse`() = runTest {
        val proxy = createProxy()

        val result = proxy.findAutocompletePredictions(
            query = "123 Main",
            country = "US",
            limit = 4,
        )

        assertThat(result.isSuccess).isTrue()
        val response = result.getOrThrow()
        assertThat(response.autocompletePredictions).hasSize(1)
        val prediction = response.autocompletePredictions.single()
        assertThat(prediction.placeId).isEqualTo("place_123")
        assertThat(prediction.primaryText.toString()).isEqualTo("123 Main St")
        assertThat(prediction.secondaryText.toString()).isEqualTo("San Francisco, CA")
    }

    @Test
    fun `findAutocompletePredictions respects limit`() = runTest {
        val repository = FakeStripeAutocompleteRepository().apply {
            predictionsResult = Result.success(
                AutocompletePredictionsResult(
                    predictions = listOf(
                        AutocompleteSuggestion("p1", "Result 1", "City, CA", null),
                        AutocompleteSuggestion("p2", "Result 2", "City, CA", null),
                        AutocompleteSuggestion("p3", "Result 3", "City, CA", null),
                    )
                )
            )
        }
        val proxy = createProxy(repository = repository)

        val result = proxy.findAutocompletePredictions(query = "123", country = "US", limit = 2)

        assertThat(result.getOrThrow().autocompletePredictions).hasSize(2)
        repository.findPredictionsCalls.awaitItem()
        repository.ensureAllEventsConsumed()
    }

    @Test
    fun `findAutocompletePredictions with null query returns empty list`() = runTest {
        val proxy = createProxy()

        val result = proxy.findAutocompletePredictions(
            query = null,
            country = "US",
            limit = 4,
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().autocompletePredictions).isEmpty()
    }

    @Test
    fun `findAutocompletePredictions returns failure on repository error`() = runTest {
        val repository = FakeStripeAutocompleteRepository().apply {
            predictionsResult = Result.failure(RuntimeException("Network error"))
        }
        val proxy = createProxy(repository = repository)

        val result = proxy.findAutocompletePredictions(query = "123 Main", country = "US", limit = 4)

        assertThat(result.isFailure).isTrue()
        repository.findPredictionsCalls.awaitItem()
        repository.ensureAllEventsConsumed()
    }

    @Test
    fun `fetchPlace uses cached address after first fetchPlaceDetails in same session`() = runTest {
        val repository = defaultRepository()
        val proxy = createProxy(repository = repository)
        proxy.findAutocompletePredictions(query = "123 Main", country = "US", limit = 4)
        repository.findPredictionsCalls.awaitItem()

        // First fetch hits the network and writes the result back to the cache.
        proxy.fetchPlace("place_123", Locale.US)
        repository.fetchPlaceDetailsCalls.awaitItem()

        // Second fetch for the same placeId must not call the repository again.
        val result = proxy.fetchPlace("place_123", Locale.US)

        assertThat(result.isSuccess).isTrue()
        repository.ensureAllEventsConsumed()
    }

    @Test
    fun `fetchPlace calls repository when no inline address cached`() = runTest {
        val repository = defaultRepository()
        val proxy = createProxy(repository = repository)
        proxy.findAutocompletePredictions(query = "123 Main", country = "US", limit = 4)
        repository.findPredictionsCalls.awaitItem()

        val result = proxy.fetchPlace("place_123", Locale.US)

        assertThat(result.isSuccess).isTrue()
        repository.fetchPlaceDetailsCalls.awaitItem()
        repository.ensureAllEventsConsumed()
    }

    @Test
    fun `fetchPlace returns correct Address from cached inline address`() = runTest {
        val inlineAddress = StripeProxyAddress(
            line1 = "123 Main St",
            line2 = "Apt 4",
            city = "San Francisco",
            state = "CA",
            postalCode = "94105",
            country = "US",
        )
        val repository = FakeStripeAutocompleteRepository().apply {
            predictionsResult = Result.success(
                AutocompletePredictionsResult(
                    predictions = listOf(
                        AutocompleteSuggestion("place_123", "123 Main St", "SF, CA", inlineAddress)
                    )
                )
            )
        }
        val proxy = createProxy(repository = repository)
        proxy.findAutocompletePredictions(query = "123", country = "US", limit = 4)
        repository.findPredictionsCalls.awaitItem()

        val result = proxy.fetchPlace("place_123", Locale.US)

        assertThat(result.getOrThrow()).isEqualTo(
            Address(
                city = "San Francisco",
                country = "US",
                line1 = "123 Main St",
                line2 = "Apt 4",
                postalCode = "94105",
                state = "CA",
            )
        )
        repository.ensureAllEventsConsumed()
    }

    @Test
    fun `fetchPlace preserves Japanese address lines directly`() = runTest {
        val inlineAddress = StripeProxyAddress(
            line1 = "3-chome-6-1 Kameido Koto City",
            line2 = "Unit 201",
            city = "Koto City",
            state = "Tokyo",
            postalCode = "136-0071",
            country = "JP",
        )
        val repository = FakeStripeAutocompleteRepository().apply {
            predictionsResult = Result.success(
                AutocompletePredictionsResult(
                    predictions = listOf(
                        AutocompleteSuggestion("place_jp", "Kameido", "Koto City, Tokyo", inlineAddress)
                    )
                )
            )
        }
        val proxy = createProxy(repository = repository)
        proxy.findAutocompletePredictions(query = "Kameido", country = "JP", limit = 4)
        repository.findPredictionsCalls.awaitItem()

        val result = proxy.fetchPlace("place_jp", Locale.getDefault())

        assertThat(result.getOrThrow()).isEqualTo(
            Address(
                city = "Koto City",
                country = "JP",
                line1 = "3-chome-6-1 Kameido Koto City",
                line2 = "Unit 201",
                postalCode = "136-0071",
                state = "Tokyo",
            )
        )
        repository.ensureAllEventsConsumed()
    }

    @Test
    fun `resetSession clears prediction cache`() = runTest {
        val inlineAddress = StripeProxyAddress(
            line1 = "123 Main St", line2 = null, city = null, state = null, postalCode = null, country = null
        )
        val repository = FakeStripeAutocompleteRepository().apply {
            predictionsResult = Result.success(
                AutocompletePredictionsResult(
                    predictions = listOf(AutocompleteSuggestion("place_123", "123 Main St", "", inlineAddress))
                )
            )
        }
        val proxy = createProxy(repository = repository)
        proxy.findAutocompletePredictions(query = "123", country = "US", limit = 4)
        repository.findPredictionsCalls.awaitItem()

        proxy.resetSession()

        proxy.fetchPlace("place_123", Locale.US)
        repository.fetchPlaceDetailsCalls.awaitItem()
        repository.ensureAllEventsConsumed()
    }

    @Test
    fun `findAutocompletePredictions and fetchPlaceDetails share the same session token within a session`() = runTest {
        val repository = defaultRepository()
        val proxy = createProxy(repository = repository)

        proxy.findAutocompletePredictions(query = "123 Main", country = "US", limit = 4)
        val findCall = repository.findPredictionsCalls.awaitItem()

        proxy.fetchPlace("place_123", Locale.US)
        val detailsCall = repository.fetchPlaceDetailsCalls.awaitItem()

        assertThat(findCall.sessionToken).isEqualTo(detailsCall.sessionToken)
        repository.ensureAllEventsConsumed()
    }

    @Test
    fun `fetchPlaceDetails token rotates after resetSession`() = runTest {
        val repository = defaultRepository()
        val proxy = createProxy(repository = repository)

        proxy.findAutocompletePredictions(query = "123 Main", country = "US", limit = 4)
        repository.findPredictionsCalls.awaitItem()
        proxy.fetchPlace("place_123", Locale.US)
        val firstDetailsCall = repository.fetchPlaceDetailsCalls.awaitItem()

        proxy.resetSession()

        proxy.findAutocompletePredictions(query = "456 Oak", country = "US", limit = 4)
        repository.findPredictionsCalls.awaitItem()
        proxy.fetchPlace("place_123", Locale.US)
        val secondDetailsCall = repository.fetchPlaceDetailsCalls.awaitItem()

        assertThat(firstDetailsCall.sessionToken).isNotEqualTo(secondDetailsCall.sessionToken)
        repository.ensureAllEventsConsumed()
    }

    @Test
    fun `resetSession generates new session token for subsequent requests`() = runTest {
        val repository = defaultRepository()
        val proxy = createProxy(repository = repository)

        proxy.findAutocompletePredictions(query = "123 Main", country = "US", limit = 4)
        val firstCall = repository.findPredictionsCalls.awaitItem()

        proxy.resetSession()

        proxy.findAutocompletePredictions(query = "456 Oak", country = "US", limit = 4)
        val secondCall = repository.findPredictionsCalls.awaitItem()

        assertThat(firstCall.sessionToken).isNotEqualTo(secondCall.sessionToken)
        repository.ensureAllEventsConsumed()
    }

    private fun defaultRepository() = FakeStripeAutocompleteRepository().apply {
        predictionsResult = Result.success(
            AutocompletePredictionsResult(
                predictions = listOf(
                    AutocompleteSuggestion("place_123", "123 Main St", "San Francisco, CA", null)
                )
            )
        )
        detailsResult = Result.success(
            PlaceDetailsResult(
                address = StripeProxyAddress(
                    line1 = "123 Main St",
                    line2 = null,
                    city = "San Francisco",
                    state = "CA",
                    postalCode = "94105",
                    country = "US",
                )
            )
        )
    }

    @Test
    fun `constructing the proxy fires onAutocompleteSessionStarted with the initial token`() = runTest {
        val eventReporter = FakeAddressLauncherEventReporter()
        createProxy(eventReporter = eventReporter)

        val token = eventReporter.autocompleteSessionStartedCalls.awaitItem()
        assertThat(token).isNotEmpty()
        eventReporter.validate()
    }

    @Test
    fun `resetSession fires onAutocompleteSessionStarted with a new token`() = runTest {
        val eventReporter = FakeAddressLauncherEventReporter()
        val proxy = createProxy(eventReporter = eventReporter)
        val initialToken = eventReporter.autocompleteSessionStartedCalls.awaitItem()

        proxy.resetSession()

        val newToken = eventReporter.autocompleteSessionStartedCalls.awaitItem()
        assertThat(newToken).isNotEqualTo(initialToken)
        eventReporter.validate()
    }

    @Test
    fun `findAutocompletePredictions success fires suggestions returned`() = runTest {
        val eventReporter = FakeAddressLauncherEventReporter()
        val repository = defaultRepository()
        val proxy = createProxy(repository = repository, eventReporter = eventReporter)
        eventReporter.autocompleteSessionStartedCalls.awaitItem()

        proxy.findAutocompletePredictions(query = "123 Main", country = "US", limit = 4)
        repository.findPredictionsCalls.awaitItem()
        eventReporter.autocompleteFetchStartedCalls.awaitItem()

        val suggestionsCall = eventReporter.autocompleteSuggestionsReturnedCalls.awaitItem()
        assertThat(suggestionsCall.resultCount).isEqualTo(1)
        repository.ensureAllEventsConsumed()
        eventReporter.validate()
    }

    @Test
    fun `findAutocompletePredictions failure fires onAutocompleteError`() = runTest {
        val eventReporter = FakeAddressLauncherEventReporter()
        val repository = FakeStripeAutocompleteRepository().apply {
            predictionsResult = Result.failure(RuntimeException("Network error"))
        }
        val proxy = createProxy(repository = repository, eventReporter = eventReporter)
        eventReporter.autocompleteSessionStartedCalls.awaitItem()

        proxy.findAutocompletePredictions(query = "123 Main", country = "US", limit = 4)
        repository.findPredictionsCalls.awaitItem()
        eventReporter.autocompleteFetchStartedCalls.awaitItem()

        val errorCall = eventReporter.autocompleteErrorCalls.awaitItem()
        assertThat(errorCall.error).hasMessageThat().isEqualTo("Network error")
        repository.ensureAllEventsConsumed()
        eventReporter.validate()
    }

    @Test
    fun `fetchPlace success fires onAutocompleteSelected with query length and place id`() = runTest {
        val eventReporter = FakeAddressLauncherEventReporter()
        val repository = defaultRepository()
        val proxy = createProxy(repository = repository, eventReporter = eventReporter)
        eventReporter.autocompleteSessionStartedCalls.awaitItem()

        proxy.findAutocompletePredictions(query = "123 Main", country = "US", limit = 4)
        repository.findPredictionsCalls.awaitItem()
        eventReporter.autocompleteFetchStartedCalls.awaitItem()
        eventReporter.autocompleteSuggestionsReturnedCalls.awaitItem()

        proxy.fetchPlace("place_123", Locale.US)
        repository.fetchPlaceDetailsCalls.awaitItem()

        val selectedCall = eventReporter.autocompleteSelectedCalls.awaitItem()
        assertThat(selectedCall.queryLength).isEqualTo("123 Main".length)
        assertThat(selectedCall.placeId).isEqualTo("place_123")
        repository.ensureAllEventsConsumed()
        eventReporter.validate()
    }

    @Test
    fun `fetchPlace failure fires onAutocompleteError`() = runTest {
        val eventReporter = FakeAddressLauncherEventReporter()
        val repository = defaultRepository().apply {
            detailsResult = Result.failure(RuntimeException("Details error"))
        }
        val proxy = createProxy(repository = repository, eventReporter = eventReporter)
        eventReporter.autocompleteSessionStartedCalls.awaitItem()

        proxy.findAutocompletePredictions(query = "123 Main", country = "US", limit = 4)
        repository.findPredictionsCalls.awaitItem()
        eventReporter.autocompleteFetchStartedCalls.awaitItem()
        eventReporter.autocompleteSuggestionsReturnedCalls.awaitItem()

        proxy.fetchPlace("place_123", Locale.US)
        repository.fetchPlaceDetailsCalls.awaitItem()

        eventReporter.autocompleteSelectedCalls.awaitItem()
        val errorCall = eventReporter.autocompleteErrorCalls.awaitItem()
        assertThat(errorCall.error).hasMessageThat().isEqualTo("Details error")
        repository.ensureAllEventsConsumed()
        eventReporter.validate()
    }

    private fun createProxy(
        repository: FakeStripeAutocompleteRepository = defaultRepository(),
        eventReporter: FakeAddressLauncherEventReporter = FakeAddressLauncherEventReporter(),
    ) = StripeHostedPlacesClientProxy(
        repository = repository,
        eventReporter = eventReporter,
    )
}
