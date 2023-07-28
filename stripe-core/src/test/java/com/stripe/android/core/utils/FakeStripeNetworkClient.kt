package com.stripe.android.core.utils

import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import kotlinx.coroutines.channels.Channel
import java.io.File

internal class FakeStripeNetworkClient : StripeNetworkClient {

    private val channel = Channel<Result<StripeResponse<String>>>(capacity = 1)

    fun enqueueResult(result: Result<StripeResponse<String>>) {
        channel.trySend(result)
    }

    override suspend fun executeRequest(request: StripeRequest): StripeResponse<String> {
        val result = channel.receive()
        return result.getOrThrow()
    }

    override suspend fun executeRequestForFile(
        request: StripeRequest,
        outputFile: File
    ): StripeResponse<File> {
        error("Not expected to be called")
    }
}
