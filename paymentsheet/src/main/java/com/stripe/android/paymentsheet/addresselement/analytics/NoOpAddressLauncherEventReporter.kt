package com.stripe.android.paymentsheet.addresselement.analytics

internal object NoOpAddressLauncherEventReporter : AddressLauncherEventReporter {
    override fun onShow(country: String) = Unit
    override fun onCompleted(
        country: String,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    ) = Unit
    override fun onAutocompleteSessionStarted(sessionToken: String, country: String) = Unit
    override fun onAutocompleteFetchStarted() = Unit
    override fun onAutocompleteSuggestionsReturned(
        sessionToken: String,
        country: String,
        resultCount: Int,
        source: String?,
    ) = Unit
    override fun onAutocompleteDetailsFetchStarted() = Unit
    override fun onAutocompleteSelected(
        sessionToken: String,
        country: String,
        queryLength: Int,
        placeId: String?,
        source: String?,
    ) = Unit
    override fun onAutocompleteError(sessionToken: String, country: String, error: Throwable) = Unit
}
