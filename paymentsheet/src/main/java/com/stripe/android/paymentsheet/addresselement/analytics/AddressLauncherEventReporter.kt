package com.stripe.android.paymentsheet.addresselement.analytics

internal interface AddressLauncherEventReporter {
    fun onShow(country: String)

    fun onCompleted(
        country: String,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    )

    fun onAutocompleteSessionStarted(country: String, sessionToken: String)

    fun onAutocompleteFetchStarted()

    fun onAutocompleteSuggestionsReturned(
        country: String,
        sessionToken: String,
        resultCount: Int,
        source: String?,
    )

    fun onAutocompleteDetailsFetchStarted()

    fun onAutocompleteSelected(
        country: String,
        sessionToken: String,
        queryLength: Int,
        placeId: String?,
        source: String?,
    )

    fun onAutocompleteError(country: String, sessionToken: String, error: Throwable)
}
