package com.stripe.android.common.nfcscan.analytics

import app.cash.turbine.Turbine

internal class FakeNfcScanningEventReporter : NfcScanningEventReporter {
    val onNfcScanStartedCalls = Turbine<Unit>()
    val onNfcScanAttemptStartedCalls = Turbine<Unit>()
    val onNfcScanAttemptSucceededCalls = Turbine<Unit>()
    val onNfcScanAttemptFailedCalls = Turbine<NfcScanAttemptFailedCall>()
    val onNfcScanSucceededCalls = Turbine<Unit>()
    val onNfcScanCancelledCalls = Turbine<NfcScanCancellationReason>()

    data class NfcScanAttemptFailedCall(
        val errorCode: String,
        val parameters: Map<String, String>,
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

    override fun onNfcScanAttemptFailed(
        errorCode: String,
        parameters: Map<String, String>,
    ) {
        onNfcScanAttemptFailedCalls.add(
            NfcScanAttemptFailedCall(
                errorCode = errorCode,
                parameters = parameters,
            ),
        )
    }

    override fun onNfcScanSucceeded() {
        onNfcScanSucceededCalls.add(Unit)
    }

    override fun onNfcScanCancelled(reason: NfcScanCancellationReason) {
        onNfcScanCancelledCalls.add(reason)
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
