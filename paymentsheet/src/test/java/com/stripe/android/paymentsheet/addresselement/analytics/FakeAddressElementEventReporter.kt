package com.stripe.android.paymentsheet.addresselement.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

internal class FakeAddressElementEventReporter : AddressElementEventReporter {
    private val _shownCalls = Turbine<AddressElementAnalyticsSnapshot>()
    val shownCalls: ReceiveTurbine<AddressElementAnalyticsSnapshot> = _shownCalls

    private val _canceledCalls = Turbine<AddressElementAnalyticsSnapshot>()
    val canceledCalls: ReceiveTurbine<AddressElementAnalyticsSnapshot> = _canceledCalls

    private val _saveStartedCalls = Turbine<AddressElementAnalyticsSnapshot>()
    val saveStartedCalls: ReceiveTurbine<AddressElementAnalyticsSnapshot> = _saveStartedCalls

    private val _saveFailedCalls = Turbine<SaveFailedCall>()
    val saveFailedCalls: ReceiveTurbine<SaveFailedCall> = _saveFailedCalls

    private val _saveCompletedCalls = Turbine<AddressElementAnalyticsSnapshot>()
    val saveCompletedCalls: ReceiveTurbine<AddressElementAnalyticsSnapshot> = _saveCompletedCalls

    override fun onShown(snapshot: AddressElementAnalyticsSnapshot) {
        _shownCalls.add(snapshot)
    }

    override fun onCanceled(snapshot: AddressElementAnalyticsSnapshot) {
        _canceledCalls.add(snapshot)
    }

    override fun onSaveStarted(snapshot: AddressElementAnalyticsSnapshot) {
        _saveStartedCalls.add(snapshot)
    }

    override fun onSaveFailed(snapshot: AddressElementAnalyticsSnapshot, error: Throwable) {
        _saveFailedCalls.add(SaveFailedCall(snapshot, error))
    }

    override fun onSaveCompleted(snapshot: AddressElementAnalyticsSnapshot) {
        _saveCompletedCalls.add(snapshot)
    }

    fun ensureAllEventsConsumed() {
        _shownCalls.ensureAllEventsConsumed()
        _canceledCalls.ensureAllEventsConsumed()
        _saveStartedCalls.ensureAllEventsConsumed()
        _saveFailedCalls.ensureAllEventsConsumed()
        _saveCompletedCalls.ensureAllEventsConsumed()
    }

    data class SaveFailedCall(
        val snapshot: AddressElementAnalyticsSnapshot,
        val error: Throwable,
    )
}
