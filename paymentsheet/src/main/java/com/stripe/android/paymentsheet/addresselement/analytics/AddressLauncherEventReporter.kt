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

    /** Call when `StripeHostedPlacesClientProxy` creates a new autocomplete session token. */
    fun onAutocompleteSessionStarted(sessionToken: String)

    /** Call immediately before `StripeHostedPlacesClientProxy` issues a prediction request. */
    fun onAutocompleteFetchStarted()

    /** Call after a `StripeHostedPlacesClientProxy` prediction request succeeds. */
    fun onAutocompleteSuggestionsReturned(sessionToken: String, resultCount: Int)

    /** Call after a `StripeHostedPlacesClientProxy` suggestion selection populates the fields. */
    fun onAutocompleteSelected(sessionToken: String, queryLength: Int, placeId: String?)

    /** Call after a `StripeHostedPlacesClientProxy` prediction or place-details request fails. */
    fun onAutocompleteError(sessionToken: String, error: Throwable)
}
