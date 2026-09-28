package com.stripe.android.paymentsheet.addresselement.analytics

internal interface AddressLauncherEventReporter {
    fun onShow(country: String)

    fun onCompleted(
        country: String,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    )

    fun onAutocompleteSessionStarted(sessionToken: String)

    fun onAutocompleteFetchStarted()

    fun onAutocompleteSuggestionsReturned(
        sessionToken: String,
        resultCount: Int,
        source: String?,
    )

    fun onAutocompleteDetailsFetchStarted()

    fun onAutocompleteSelected(sessionToken: String, queryLength: Int, placeId: String?, source: String?)

    fun onAutocompleteError(sessionToken: String, error: Throwable)
}
