package com.stripe.android.paymentsheet.addresselement.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import kotlin.time.Duration

internal class FakeAddressLauncherEventReporter : AddressLauncherEventReporter {
    private val _showCalls = Turbine<String>()
    val showCalls: ReceiveTurbine<String> = _showCalls

    private val _completedCalls = Turbine<CompletedCall>()
    val completedCalls: ReceiveTurbine<CompletedCall> = _completedCalls

    private val _autocompleteSessionStartedCalls = Turbine<String>()
    val autocompleteSessionStartedCalls: ReceiveTurbine<String> = _autocompleteSessionStartedCalls

    private val _autocompleteFetchStartedCalls = Turbine<Unit>()
    val autocompleteFetchStartedCalls: ReceiveTurbine<Unit> = _autocompleteFetchStartedCalls

    private val _autocompleteSuggestionsReturnedCalls = Turbine<SuggestionsReturnedCall>()
    val autocompleteSuggestionsReturnedCalls: ReceiveTurbine<SuggestionsReturnedCall> =
        _autocompleteSuggestionsReturnedCalls

    private val _autocompleteSelectedCalls = Turbine<SelectedCall>()
    val autocompleteSelectedCalls: ReceiveTurbine<SelectedCall> = _autocompleteSelectedCalls

    private val _autocompleteErrorCalls = Turbine<ErrorCall>()
    val autocompleteErrorCalls: ReceiveTurbine<ErrorCall> = _autocompleteErrorCalls

    override fun onShow(country: String) {
        _showCalls.add(country)
    }

    override fun onCompleted(
        country: String,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
        timeToComplete: Duration?,
    ) {
        _completedCalls.add(CompletedCall(country, autocompleteResultSelected, editDistance, timeToComplete))
    }

    override fun onAutocompleteSessionStarted(sessionToken: String) {
        _autocompleteSessionStartedCalls.add(sessionToken)
    }

    override fun onAutocompleteFetchStarted() {
        _autocompleteFetchStartedCalls.add(Unit)
    }

    override fun onAutocompleteSuggestionsReturned(sessionToken: String, resultCount: Int) {
        _autocompleteSuggestionsReturnedCalls.add(SuggestionsReturnedCall(sessionToken, resultCount))
    }

    override fun onAutocompleteSelected(sessionToken: String, queryLength: Int, placeId: String?) {
        _autocompleteSelectedCalls.add(SelectedCall(sessionToken, queryLength, placeId))
    }

    override fun onAutocompleteError(sessionToken: String, error: Throwable) {
        _autocompleteErrorCalls.add(ErrorCall(sessionToken, error))
    }

    fun validate() {
        _showCalls.ensureAllEventsConsumed()
        _completedCalls.ensureAllEventsConsumed()
        _autocompleteSessionStartedCalls.ensureAllEventsConsumed()
        _autocompleteFetchStartedCalls.ensureAllEventsConsumed()
        _autocompleteSuggestionsReturnedCalls.ensureAllEventsConsumed()
        _autocompleteSelectedCalls.ensureAllEventsConsumed()
        _autocompleteErrorCalls.ensureAllEventsConsumed()
    }

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
