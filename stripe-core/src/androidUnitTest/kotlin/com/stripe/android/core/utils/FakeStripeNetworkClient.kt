package com.stripe.android.core.utils

import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import okio.Path

internal class FakeStripeNetworkClient(
    private val executeRequest: () -> StripeResponse<String> = { StripeResponse(200, null) },
    private val executeRequestForFile: () -> StripeResponse<Path> = { StripeResponse(200, null) },
) : StripeNetworkClient {

    var executeRequestCalled: Boolean = false
        private set

    override suspend fun executeRequest(request: StripeRequest): StripeResponse<String> {
        executeRequestCalled = true
        return executeRequest()
    }

    override suspend fun executeRequestForFile(request: StripeRequest, outputFile: Path): StripeResponse<Path> {
        return executeRequestForFile()
    }
}
