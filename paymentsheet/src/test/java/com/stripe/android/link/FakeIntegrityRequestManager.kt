package com.stripe.android.link

import app.cash.turbine.Turbine
import com.stripe.attestation.IntegrityRequestManager

internal class FakeIntegrityRequestManager : IntegrityRequestManager {
    var prepareResult: Result<Unit> = Result.success(Unit)
    var requestResult: Result<String> = Result.success(TestFactory.VERIFICATION_TOKEN)
    private val prepareCalls = Turbine<Unit>()
    private val requestTokenCalls = Turbine<String?>()

    override suspend fun prepare(): Result<Unit> {
        prepareCalls.add(Unit)
        return prepareResult
    }

    override suspend fun requestToken(requestIdentifier: String?): Result<String> {
        requestTokenCalls.add(requestIdentifier)
        return requestResult
    }

    suspend fun awaitPrepareCall() {
        return prepareCalls.awaitItem()
    }

    suspend fun awaitRequestTokenCall(): String? {
        return requestTokenCalls.awaitItem()
    }

    fun ensureAllEventsConsumed() {
        prepareCalls.ensureAllEventsConsumed()
        requestTokenCalls.ensureAllEventsConsumed()
    }
}
