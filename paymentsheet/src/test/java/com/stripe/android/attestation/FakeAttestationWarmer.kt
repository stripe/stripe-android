package com.stripe.android.attestation

import app.cash.turbine.Turbine
import com.stripe.attestation.AttestationWarmer

internal class FakeAttestationWarmer(
    var result: Result<Unit> = Result.success(Unit)
) : AttestationWarmer {
    private val startCalls = Turbine<Unit>()

    override suspend fun start(): Result<Unit> {
        startCalls.add(Unit)
        return result
    }

    suspend fun awaitStartCall() {
        startCalls.awaitItem()
    }

    fun ensureAllEventsConsumed() {
        startCalls.ensureAllEventsConsumed()
    }
}
