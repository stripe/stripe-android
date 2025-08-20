package com.stripe.android.ui.core.cardscan

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

internal class FakeCardScanEventsReporter : CardScanEventsReporter {
    data class ScanCancelledCall(val implementation: String)
    data class ScanFailedCall(val implementation: String, val error: Throwable?)
    data class ScanSucceededCall(val implementation: String)
    data class ScanStartedCall(val implementation: String)
    data class ApiCheckSucceededCall(val implementation: String)
    data class ApiCheckFailedCall(val implementation: String, val error: Throwable?)

    private val _scanCancelledCalls = Turbine<ScanCancelledCall>()
    val scanCancelledCalls: ReceiveTurbine<ScanCancelledCall> = _scanCancelledCalls

    private val _scanFailedCalls = Turbine<ScanFailedCall>()
    val scanFailedCalls: ReceiveTurbine<ScanFailedCall> = _scanFailedCalls

    private val _scanSucceededCalls = Turbine<ScanSucceededCall>()
    val scanSucceededCalls: ReceiveTurbine<ScanSucceededCall> = _scanSucceededCalls

    private val _scanStartedCalls = Turbine<ScanStartedCall>()
    val scanStartedCalls: ReceiveTurbine<ScanStartedCall> = _scanStartedCalls

    private val _apiCheckSucceededCalls = Turbine<ApiCheckSucceededCall>()
    val apiCheckSucceededCalls: ReceiveTurbine<ApiCheckSucceededCall> = _apiCheckSucceededCalls

    private val _apiCheckFailedCalls = Turbine<ApiCheckFailedCall>()
    val apiCheckFailedCalls: ReceiveTurbine<ApiCheckFailedCall> = _apiCheckFailedCalls

    fun validate() {
        _scanCancelledCalls.ensureAllEventsConsumed()
        _scanFailedCalls.ensureAllEventsConsumed()
        _scanSucceededCalls.ensureAllEventsConsumed()
        _scanStartedCalls.ensureAllEventsConsumed()
        _apiCheckSucceededCalls.ensureAllEventsConsumed()
        _apiCheckFailedCalls.ensureAllEventsConsumed()
    }

    override fun onCardScanCancelled(implementation: String) {
        _scanCancelledCalls.add(ScanCancelledCall(implementation))
    }

    override fun onCardScanFailed(implementation: String, error: Throwable?) {
        _scanFailedCalls.add(ScanFailedCall(implementation, error))
    }

    override fun onCardScanSucceeded(implementation: String) {
        _scanSucceededCalls.add(ScanSucceededCall(implementation))
    }

    override fun onCardScanStarted(implementation: String) {
        _scanStartedCalls.add(ScanStartedCall(implementation))
    }

    override fun onCardScanApiCheckSucceeded(implementation: String) {
        _apiCheckSucceededCalls.add(ApiCheckSucceededCall(implementation))
    }

    override fun onCardScanApiCheckFailed(implementation: String, error: Throwable?) {
        _apiCheckFailedCalls.add(ApiCheckFailedCall(implementation, error))
    }
}
