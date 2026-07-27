package com.stripe.android.paymentsheet.addresselement.analytics

import app.cash.turbine.Turbine
import kotlin.time.Duration

internal class FakeAddressLauncherEventReporter : AddressLauncherEventReporter {
    val showCalls = Turbine<ShowCall>()
    val completedCalls = Turbine<CompletedCall>()
    val autocompleteSessionStartedCalls = Turbine<String>()
    val autocompleteFetchStartedCalls = Turbine<Unit>()
    val autocompleteSuggestionsReturnedCalls = Turbine<SuggestionsReturnedCall>()
    val autocompleteSelectedCalls = Turbine<SelectedCall>()
    val autocompleteErrorCalls = Turbine<ErrorCall>()

    override fun onShow(country: String) {
        showCalls.add(ShowCall(country))
    }

    override fun onCompleted(
        country: String,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
        timeToComplete: Duration?,
    ) {
        completedCalls.add(CompletedCall(country, autocompleteResultSelected, editDistance, timeToComplete))
    }

    override fun onAutocompleteSessionStarted(sessionToken: String) {
        autocompleteSessionStartedCalls.add(sessionToken)
    }

    override fun onAutocompleteFetchStarted() {
        autocompleteFetchStartedCalls.add(Unit)
    }

    override fun onAutocompleteSuggestionsReturned(sessionToken: String, resultCount: Int) {
        autocompleteSuggestionsReturnedCalls.add(SuggestionsReturnedCall(sessionToken, resultCount))
    }

    override fun onAutocompleteSelected(sessionToken: String, queryLength: Int, placeId: String?) {
        autocompleteSelectedCalls.add(SelectedCall(sessionToken, queryLength, placeId))
    }

    override fun onAutocompleteError(sessionToken: String, error: Throwable) {
        autocompleteErrorCalls.add(ErrorCall(sessionToken, error))
    }

    fun ensureAllEventsConsumed() {
        showCalls.ensureAllEventsConsumed()
        completedCalls.ensureAllEventsConsumed()
        autocompleteSessionStartedCalls.ensureAllEventsConsumed()
        autocompleteFetchStartedCalls.ensureAllEventsConsumed()
        autocompleteSuggestionsReturnedCalls.ensureAllEventsConsumed()
        autocompleteSelectedCalls.ensureAllEventsConsumed()
        autocompleteErrorCalls.ensureAllEventsConsumed()
    }

    data class ShowCall(val country: String)

    data class CompletedCall(
        val country: String,
        val autocompleteResultSelected: Boolean,
        val editDistance: Int?,
        val timeToComplete: Duration?,
    )

    data class SuggestionsReturnedCall(val sessionToken: String, val resultCount: Int)

    data class SelectedCall(val sessionToken: String, val queryLength: Int, val placeId: String?)

    data class ErrorCall(val sessionToken: String, val error: Throwable)
}
