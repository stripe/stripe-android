package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse

internal class StripeHostedPlacesClientProxy : PlacesClientProxy {
    override suspend fun findAutocompletePredictions(
        query: String?,
        country: String,
        limit: Int
    ): Result<FindAutocompletePredictionsResponse> {
        return Result.failure(
            NotImplementedError("Stripe-hosted autocomplete not yet implemented")
        )
    }

    override suspend fun fetchPlace(placeId: String): Result<FetchPlaceResponse> {
        return Result.failure(
            NotImplementedError("Stripe-hosted place details not yet implemented")
        )
    }
}
