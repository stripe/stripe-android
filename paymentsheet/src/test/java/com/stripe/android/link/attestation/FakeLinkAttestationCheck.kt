package com.stripe.android.link.attestation

import app.cash.turbine.Turbine

internal class FakeLinkAttestationCheck : LinkAttestationCheck {
    var result: LinkAttestationCheck.Result = LinkAttestationCheck.Result.Successful
    private val calls = Turbine<Unit>()

    override suspend fun invoke(): LinkAttestationCheck.Result {
        calls.add(Unit)
        return result
    }

    suspend fun awaitInvokeCall() {
        calls.awaitItem()
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }
}
