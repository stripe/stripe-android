package com.stripe.android.paymentsheet.addresselement.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

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

    private val _autocompleteDetailsFetchStartedCalls = Turbine<Unit>()
    val autocompleteDetailsFetchStartedCalls: ReceiveTurbine<Unit> = _autocompleteDetailsFetchStartedCalls

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
    ) {
        _completedCalls.add(CompletedCall(country, autocompleteResultSelected, editDistance))
    }

    override fun onAutocompleteSessionStarted(sessionToken: String) {
        _autocompleteSessionStartedCalls.add(sessionToken)
    }

    override fun onAutocompleteFetchStarted() {
        _autocompleteFetchStartedCalls.add(Unit)
    }

    override fun onAutocompleteSuggestionsReturned(
        sessionToken: String,
        queryLength: Int,
        resultCount: Int,
        source: String?,
    ) {
        _autocompleteSuggestionsReturnedCalls.add(
            SuggestionsReturnedCall(sessionToken, queryLength, resultCount, source)
        )
    }

    override fun onAutocompleteDetailsFetchStarted() {
        _autocompleteDetailsFetchStartedCalls.add(Unit)
    }

    override fun onAutocompleteSelected(sessionToken: String, queryLength: Int, placeId: String?, source: String?) {
        _autocompleteSelectedCalls.add(SelectedCall(sessionToken, queryLength, placeId, source))
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
        _autocompleteDetailsFetchStartedCalls.ensureAllEventsConsumed()
        _autocompleteSelectedCalls.ensureAllEventsConsumed()
        _autocompleteErrorCalls.ensureAllEventsConsumed()
    }

    data class CompletedCall(
        val country: String,
        val autocompleteResultSelected: Boolean,
        val editDistance: Int?,
    )

    data class SuggestionsReturnedCall(
        val sessionToken: String,
        val queryLength: Int,
        val resultCount: Int,
        val source: String?,
    )

    data class SelectedCall(val sessionToken: String, val queryLength: Int, val placeId: String?, val source: String?)

    data class ErrorCall(val sessionToken: String, val error: Throwable)
}
