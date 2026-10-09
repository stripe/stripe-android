package com.stripe.android.attestation

import app.cash.turbine.Turbine
import com.stripe.attestation.AttestationTokenProvider

internal class FakeAttestationTokenProvider(
    var result: Result<String>
) : AttestationTokenProvider {
    private val getTokenCalls = Turbine<Unit>()

    override suspend fun getToken(): Result<String> {
        getTokenCalls.add(Unit)
        return result
    }

    suspend fun awaitGetTokenCall() {
        getTokenCalls.awaitItem()
    }

    fun ensureAllEventsConsumed() {
        getTokenCalls.ensureAllEventsConsumed()
    }
}
