package com.stripe.android.common.nfcscan.analytics

import app.cash.turbine.Turbine
internal class FakeNfcScanningEventReporter : NfcScanningEventReporter {
    val onNfcScanStartedCalls = Turbine<Unit>()
    val onNfcScanAttemptStartedCalls = Turbine<Unit>()
    val onNfcScanAttemptSucceededCalls = Turbine<Unit>()
    val onNfcScanAttemptFailedCalls = Turbine<Throwable>()
    val onNfcScanSucceededCalls = Turbine<Int>()
    val onNfcScanCancelledCalls = Turbine<NfcScanCancelledCall>()

    data class NfcScanCancelledCall(
        val reason: NfcScanCancellationReason,
        val numberOfAttempts: Int,
    )

    override fun onNfcScanStarted() {
        onNfcScanStartedCalls.add(Unit)
    }

    override fun onNfcScanAttemptStarted() {
        onNfcScanAttemptStartedCalls.add(Unit)
    }

    override fun onNfcScanAttemptSucceeded() {
        onNfcScanAttemptSucceededCalls.add(Unit)
    }

    override fun onNfcScanAttemptFailed(error: Throwable) {
        onNfcScanAttemptFailedCalls.add(error)
    }

    override fun onNfcScanSucceeded(numberOfAttempts: Int) {
        onNfcScanSucceededCalls.add(numberOfAttempts)
    }

    override fun onNfcScanCancelled(
        reason: NfcScanCancellationReason,
        numberOfAttempts: Int,
    ) {
        onNfcScanCancelledCalls.add(
            NfcScanCancelledCall(
                reason = reason,
                numberOfAttempts = numberOfAttempts,
            ),
        )
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
