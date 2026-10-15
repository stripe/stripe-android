package com.stripe.android.core.networking

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Test
import java.io.File
import kotlin.test.assertFailsWith

internal class RequestExecutorCancellationTest {
    @Test
    fun `result parsing preserves coroutine cancellation`() = runTest {
        val error = CancellationException("Request canceled")
        val calls = Turbine<StripeRequest>()
        val networkClient = object : StripeNetworkClient {
            override suspend fun executeRequest(request: StripeRequest): StripeResponse<String> {
                calls.add(request)
                throw error
            }

            override suspend fun executeRequestForFile(request: StripeRequest, outputFile: File): StripeResponse<File> {
                error("Unexpected file request")
            }
        }
        val request = FakeStripeRequest()
        val result = assertFailsWith<CancellationException> {
            executeRequestWithResultParser(
                stripeNetworkClient = networkClient,
                stripeErrorJsonParser = StripeErrorJsonParser(),
                request = request,
                responseJsonParser = object : ModelJsonParser<StripeModel> {
                    override fun parse(json: JSONObject): StripeModel? = error("Unexpected parsing")
                },
            )
        }
        assertThat(result).isSameInstanceAs(error)
        assertThat(calls.awaitItem()).isSameInstanceAs(request)
        calls.ensureAllEventsConsumed()
    }
}
