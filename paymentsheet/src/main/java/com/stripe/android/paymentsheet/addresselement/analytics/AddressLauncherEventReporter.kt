package com.stripe.android.paymentsheet.addresselement.analytics

internal interface AddressLauncherEventReporter {
    fun onShow(country: String)

    fun onCompleted(
        country: String,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    )

    fun onAutocompleteSessionStarted(sessionToken: String)

    fun onAutocompleteFetchStarted(sessionToken: String)

    fun onAutocompleteSuggestionsReturned(
        sessionToken: String,
        queryLength: Int,
        resultCount: Int,
    )

    fun onAutocompleteSelected(sessionToken: String, queryLength: Int, placeId: String)

    fun onAutocompleteError(sessionToken: String, error: Throwable)
}
