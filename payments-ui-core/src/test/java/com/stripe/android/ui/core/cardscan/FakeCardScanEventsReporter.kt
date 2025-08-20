package com.stripe.android.ui.core.cardscan

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

internal class FakeCardScanEventsReporter : CardScanEventsReporter {
    data class ScanCancelledCall(val implementation: String)
    data class ScanFailedCall(val implementation: String, val error: Throwable?)
    data class ScanSucceededCall(val implementation: String)
    data class ScanStartedCall(val implementation: String)
    data class ApiCheckCall(val implementation: String, val available: Boolean, val reason: String?)

    private val _scanCancelledCalls = Turbine<ScanCancelledCall>()
    val scanCancelledCalls: ReceiveTurbine<ScanCancelledCall> = _scanCancelledCalls

    private val _scanFailedCalls = Turbine<ScanFailedCall>()
    val scanFailedCalls: ReceiveTurbine<ScanFailedCall> = _scanFailedCalls

    private val _scanSucceededCalls = Turbine<ScanSucceededCall>()
    val scanSucceededCalls: ReceiveTurbine<ScanSucceededCall> = _scanSucceededCalls

    private val _scanStartedCalls = Turbine<ScanStartedCall>()
    val scanStartedCalls: ReceiveTurbine<ScanStartedCall> = _scanStartedCalls

    private val _apiCheckCalls = Turbine<ApiCheckCall>()
    val apiCheckCalls: ReceiveTurbine<ApiCheckCall> = _apiCheckCalls

    fun validate() {
        _scanCancelledCalls.ensureAllEventsConsumed()
        _scanFailedCalls.ensureAllEventsConsumed()
        _scanSucceededCalls.ensureAllEventsConsumed()
        _scanStartedCalls.ensureAllEventsConsumed()
        _apiCheckCalls.ensureAllEventsConsumed()
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

    override fun onCardScanApiCheck(implementation: String, available: Boolean, reason: String?) {
        _apiCheckCalls.add(ApiCheckCall(implementation, available, reason))
    }
}
