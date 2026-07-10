package com.stripe.android.common.nfcscan.analytics

import app.cash.turbine.Turbine

internal class FakeNfcScanningEventReporter : NfcScanningEventReporter {
    val onNfcScanStartedCalls = Turbine<Unit>()
    val onNfcScanAttemptStartedCalls = Turbine<Unit>()
    val onNfcScanAttemptSucceededCalls = Turbine<Unit>()
    val onNfcScanAttemptFailedCalls = Turbine<Unit>()
    val onNfcScanSucceededCalls = Turbine<Unit>()
    val onNfcScanCancelledCalls = Turbine<Unit>()

    override fun onNfcScanStarted() {
        onNfcScanStartedCalls.add(Unit)
    }

    override fun onNfcScanAttemptStarted() {
        onNfcScanAttemptStartedCalls.add(Unit)
    }

    override fun onNfcScanAttemptSucceeded() {
        onNfcScanAttemptSucceededCalls.add(Unit)
    }

    override fun onNfcScanAttemptFailed() {
        onNfcScanAttemptFailedCalls.add(Unit)
    }

    override fun onNfcScanSucceeded() {
        onNfcScanSucceededCalls.add(Unit)
    }

    override fun onNfcScanCancelled() {
        onNfcScanCancelledCalls.add(Unit)
    }

    fun ensureAllEventsConsumed() {
        onNfcScanStartedCalls.ensureAllEventsConsumed()
        onNfcScanAttemptStartedCalls.ensureAllEventsConsumed()
        onNfcScanAttemptSucceededCalls.ensureAllEventsConsumed()
        onNfcScanAttemptFailedCalls.ensureAllEventsConsumed()
        onNfcScanSucceededCalls.ensureAllEventsConsumed()
        onNfcScanCancelledCalls.ensureAllEventsConsumed()
    }
}
