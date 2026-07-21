package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.ui.core.elements.autocomplete.model.transformGoogleToStripeAddress
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
    fun `fetchPlace uses cached inline address when available`() = runTest {
        val inlineAddress = StripeProxyAddress(
            line1 = "123 Main St",
            line2 = null,
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
        proxy.findAutocompletePredictions(query = "123 Main", country = "US", limit = 4)
        repository.findPredictionsCalls.awaitItem()

        val result = proxy.fetchPlace("place_123")

        assertThat(result.isSuccess).isTrue()
        repository.ensureAllEventsConsumed()
    }

    @Test
    fun `fetchPlace calls repository when no inline address cached`() = runTest {
        val repository = defaultRepository()
        val proxy = createProxy(repository = repository)
        proxy.findAutocompletePredictions(query = "123 Main", country = "US", limit = 4)
        repository.findPredictionsCalls.awaitItem()

        val result = proxy.fetchPlace("place_123")

        assertThat(result.isSuccess).isTrue()
        repository.fetchPlaceDetailsCalls.awaitItem()
        repository.ensureAllEventsConsumed()
    }

    @Test
    fun `fetchPlace inline address produces correct FetchPlaceResponse fields`() = runTest {
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

        val result = proxy.fetchPlace("place_123")

        val place = result.getOrThrow().place
        val routeComponent = place.addressComponents?.find { it.types.contains("route") }
        val premiseComponent = place.addressComponents?.find { it.types.contains("premise") }
        val localityComponent = place.addressComponents?.find { it.types.contains("locality") }
        val stateComponent = place.addressComponents?.find { it.types.contains("administrative_area_level_1") }
        val postalComponent = place.addressComponents?.find { it.types.contains("postal_code") }
        val countryComponent = place.addressComponents?.find { it.types.contains("country") }
        assertThat(routeComponent?.longName).isEqualTo("123 Main St")
        assertThat(premiseComponent?.longName).isEqualTo("Apt 4")
        assertThat(localityComponent?.longName).isEqualTo("San Francisco")
        assertThat(stateComponent?.shortName).isEqualTo("CA")
        assertThat(postalComponent?.longName).isEqualTo("94105")
        assertThat(countryComponent?.shortName).isEqualTo("US")
        repository.ensureAllEventsConsumed()
    }

    @Test
    fun `fetchPlace inline Japanese address preserves flattened lines`() = runTest {
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

        val result = proxy.fetchPlace("place_jp")

        assertThat(result.getOrThrow().place.transformGoogleToStripeAddress(Locale.getDefault())).isEqualTo(
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

        proxy.fetchPlace("place_123")
        repository.fetchPlaceDetailsCalls.awaitItem()
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

    private fun createProxy(
        repository: FakeStripeAutocompleteRepository = defaultRepository(),
    ) = StripeHostedPlacesClientProxy(
        repository = repository,
    )
}
