package com.stripe.android.link

import app.cash.turbine.Turbine
import com.stripe.attestation.IntegrityRequestManager

internal class FakeIntegrityRequestManager : IntegrityRequestManager {
    var requestResult: Result<String> = Result.success(TestFactory.VERIFICATION_TOKEN)
    private val requestTokenCalls = Turbine<String?>()

    override suspend fun requestToken(requestIdentifier: String?): Result<String> {
        requestTokenCalls.add(requestIdentifier)
        return requestResult
    }

    suspend fun awaitRequestTokenCall(): String? {
        return requestTokenCalls.awaitItem()
    }

    fun ensureAllEventsConsumed() {
        requestTokenCalls.ensureAllEventsConsumed()
    }
}
