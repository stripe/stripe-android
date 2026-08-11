package com.stripe.android.paymentsheet.addresselement.analytics

internal object NoOpAddressLauncherEventReporter : AddressLauncherEventReporter {
    override fun onShow(country: String) = Unit
    override fun onCompleted(
        country: String,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    ) = Unit
    override fun onAutocompleteSessionStarted(country: String, sessionToken: String) = Unit
    override fun onAutocompleteFetchStarted() = Unit
    override fun onAutocompleteSuggestionsReturned(
        country: String,
        sessionToken: String,
        resultCount: Int,
        source: String?,
    ) = Unit
    override fun onAutocompleteDetailsFetchStarted() = Unit
    override fun onAutocompleteSelected(
        country: String,
        sessionToken: String,
        queryLength: Int,
        placeId: String?,
        source: String?,
    ) = Unit
    override fun onAutocompleteError(country: String, sessionToken: String, error: Throwable) = Unit
}
