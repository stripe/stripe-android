package com.stripe.android.financialconnections.utils

import app.cash.turbine.Turbine
import com.stripe.attestation.IntegrityTokenProviderWarmer

internal class FakeIntegrityTokenProviderWarmer(
    var warmupResult: Result<Unit> = Result.success(Unit)
) : IntegrityTokenProviderWarmer {
    private val warmupCalls = Turbine<Unit>()

    override suspend fun warmup(): Result<Unit> {
        warmupCalls.add(Unit)
        return warmupResult
    }

    suspend fun awaitWarmupCall() {
        warmupCalls.awaitItem()
    }

    fun ensureAllEventsConsumed() {
        warmupCalls.ensureAllEventsConsumed()
    }
}
