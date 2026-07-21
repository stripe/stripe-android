package com.stripe.android.paymentsheet.addresselement

internal class FakeStripeAutocompleteRepository : StripeAutocompleteRepository {
    var predictionsResult: Result<AutocompletePredictionsResult> = Result.success(
        AutocompletePredictionsResult(predictions = emptyList())
    )
    var detailsResult: Result<PlaceDetailsResult> = Result.failure(NotImplementedError())
    var onBeforeFetchPlaceDetails: (suspend () -> Unit)? = null

    override suspend fun findAutocompletePredictions(
        query: String,
        country: String,
        sessionToken: String,
        locale: String?,
        googleApiKey: String?
    ): Result<AutocompletePredictionsResult> = predictionsResult

    override suspend fun fetchPlaceDetails(
        placeId: String,
        sessionToken: String
    ): Result<PlaceDetailsResult> {
        onBeforeFetchPlaceDetails?.invoke()
        return detailsResult
    }
}
