package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse

/**
 * A [PlacesClientProxy] that always fails, used by the standalone Address Element when
 * Stripe-hosted autocomplete is enabled but no proxy API service is available. Failures
 * trigger [InlineAutocompleteController.handleFailure] which expands the form for manual entry.
 */
internal class StripeHostedPlacesClientProxy : PlacesClientProxy {
    override suspend fun findAutocompletePredictions(
        query: String?,
        country: String,
        limit: Int
    ): Result<FindAutocompletePredictionsResponse> {
        return Result.failure(
            IllegalStateException("Stripe-hosted autocomplete proxy unavailable in this context")
        )
    }

    override suspend fun fetchPlace(placeId: String): Result<FetchPlaceResponse> {
        return Result.failure(
            IllegalStateException("Stripe-hosted place details proxy unavailable in this context")
        )
    }
}
