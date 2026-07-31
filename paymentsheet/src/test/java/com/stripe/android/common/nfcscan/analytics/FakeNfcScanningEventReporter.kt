package com.stripe.android.common.nfcscan.analytics

import app.cash.turbine.Turbine

internal class FakeNfcScanningEventReporter : NfcScanningEventReporter {
    val onNfcScanStartedCalls = Turbine<Unit>()
    val onNfcScanAttemptStartedCalls = Turbine<Unit>()
    val onNfcScanAttemptSucceededCalls = Turbine<Unit>()
    val onNfcScanAttemptFailedCalls = Turbine<NfcScanAttemptFailedCall>()
    val onNfcScanSucceededCalls = Turbine<Int>()
    val onNfcScanCancelledCalls = Turbine<NfcScanCancelledCall>()

    data class NfcScanCancelledCall(
        val reason: NfcScanCancellationReason,
        val numberOfAttempts: Int,
    )

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
