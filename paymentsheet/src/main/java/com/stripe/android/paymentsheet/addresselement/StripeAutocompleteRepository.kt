package com.stripe.android.paymentsheet.addresselement

internal interface StripeAutocompleteRepository {
    suspend fun findAutocompletePredictions(
        query: String,
        country: String,
        sessionToken: String,
        locale: String?,
        googleApiKey: String?
    ): Result<AutocompletePredictionsResult>

    suspend fun fetchPlaceDetails(
        placeId: String,
        sessionToken: String
    ): Result<PlaceDetailsResult>
}
