package com.stripe.android.paymentsheet.addresselement.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

internal class FakeAddressElementEventReporter : AddressElementEventReporter {
    private val _shownCalls = Turbine<ShownCall>()
    val shownCalls: ReceiveTurbine<ShownCall> = _shownCalls

    private val _canceledCalls = Turbine<AnalyticsCall>()
    val canceledCalls: ReceiveTurbine<AnalyticsCall> = _canceledCalls

    private val _saveStartedCalls = Turbine<AnalyticsCall>()
    val saveStartedCalls: ReceiveTurbine<AnalyticsCall> = _saveStartedCalls

    private val _saveFailedCalls = Turbine<SaveFailedCall>()
    val saveFailedCalls: ReceiveTurbine<SaveFailedCall> = _saveFailedCalls

    private val _saveCompletedCalls = Turbine<AnalyticsCall>()
    val saveCompletedCalls: ReceiveTurbine<AnalyticsCall> = _saveCompletedCalls

    override fun onShown(country: String?) {
        _shownCalls.add(ShownCall(country))
    }

    override fun onCanceled(
        country: String?,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    ) {
        _canceledCalls.add(AnalyticsCall(country, autocompleteResultSelected, editDistance))
    }

    override fun onSaveStarted(
        country: String?,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    ) {
        _saveStartedCalls.add(AnalyticsCall(country, autocompleteResultSelected, editDistance))
    }

    override fun onSaveFailed(
        country: String?,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
        error: Throwable,
    ) {
        _saveFailedCalls.add(
            SaveFailedCall(
                analyticsCall = AnalyticsCall(country, autocompleteResultSelected, editDistance),
                error = error,
            )
        )
    }

    override fun onSaveCompleted(
        country: String?,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    ) {
        _saveCompletedCalls.add(AnalyticsCall(country, autocompleteResultSelected, editDistance))
    }

    fun ensureAllEventsConsumed() {
        _shownCalls.ensureAllEventsConsumed()
        _canceledCalls.ensureAllEventsConsumed()
        _saveStartedCalls.ensureAllEventsConsumed()
        _saveFailedCalls.ensureAllEventsConsumed()
        _saveCompletedCalls.ensureAllEventsConsumed()
    }

    data class ShownCall(
        val country: String?,
    )

    data class AnalyticsCall(
        val country: String?,
        val autocompleteResultSelected: Boolean,
        val editDistance: Int?,
    )

    data class SaveFailedCall(
        val analyticsCall: AnalyticsCall,
        val error: Throwable,
    )
}
