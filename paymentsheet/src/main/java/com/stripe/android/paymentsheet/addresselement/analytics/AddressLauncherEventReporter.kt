package com.stripe.android.paymentsheet.addresselement.analytics

import kotlin.time.Duration

internal interface AddressLauncherEventReporter {
    fun onShow(country: String)

    fun onCompleted(
        country: String,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
        timeToComplete: Duration?,
    )

    fun onAutocompleteSessionStarted(sessionToken: String)

    fun onAutocompleteFetchStarted()

    fun onAutocompleteSuggestionsReturned(sessionToken: String, resultCount: Int)

    fun onAutocompleteSelected(sessionToken: String, queryLength: Int, placeId: String)

    fun onAutocompleteError(sessionToken: String, error: Throwable)
}
