package com.stripe.android.paymentsheet.example.playground.checkout

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Test

class PlaygroundBackendTest {
    @Test
    fun `base URL allows with or without trailing slash`() {
        assertThat(normalizedPlaygroundBaseUrl("https://example.com/")).isEqualTo("https://example.com")
        assertThat(normalizedPlaygroundBaseUrl("https://example.com")).isEqualTo("https://example.com")
    }

    @Test
    fun `base URL rejects legacy endpoints`() {
        listOf("checkout_session", "create_checkout_session").forEach { endpoint ->
            val error = runCatching {
                normalizedPlaygroundBaseUrl("https://example.com/$endpoint/")
            }.exceptionOrNull()
            assertThat(error).hasMessageThat().contains("base URL")
        }
    }

    @Test
    fun `publishable key uses merchant query and decodes response`() = runTest {
        val executor = FakePlaygroundRequestExecutor(buildJsonObject { put("publishable_key", "pk_test_123") })
        val backend = PlaygroundBackend("https://example.com/", "JP", executor)

        assertThat(backend.fetchPublishableKey()).isEqualTo("pk_test_123")
        assertThat(executor.requests.awaitItem()).isEqualTo(
            PlaygroundRequest(
                method = PlaygroundRequest.Method.Get,
                url = "https://example.com/publishable_key",
                queryParameters = listOf("merchant" to "JP"),
                body = null,
            )
        )
        executor.ensureAllEventsConsumed()
    }

    @Test
    fun `customer and attachment use generic envelopes`() = runTest {
        val executor = FakePlaygroundRequestExecutor(buildJsonObject { put("id", "cus_123") })
        val backend = PlaygroundBackend("https://example.com", "US", executor)

        assertThat(backend.createCustomer(buildJsonObject { put("email", "a@example.com") })).isEqualTo("cus_123")
        val customerRequest = executor.requests.awaitItem()
        assertThat(customerRequest.url).isEqualTo("https://example.com/create_customer")
        assertThat(customerRequest.body!!.string("merchant")).isEqualTo("US")
        assertThat(customerRequest.body["request_params"]!!.jsonObject.string("email")).isEqualTo("a@example.com")

        executor.response = buildJsonObject { }
        backend.attachPaymentMethod("pm_123", "cus_123")
        val attachRequest = executor.requests.awaitItem()
        assertThat(attachRequest.url).isEqualTo("https://example.com/attach_payment_method")
        assertThat(attachRequest.body!!.string("payment_method_id")).isEqualTo("pm_123")
        assertThat(attachRequest.body["request_params"]!!.jsonObject.string("customer")).isEqualTo("cus_123")
        executor.ensureAllEventsConsumed()
    }

    @Test
    fun `checkout session sends preview version and decodes secret`() = runTest {
        val executor = FakePlaygroundRequestExecutor(buildJsonObject { put("client_secret", "cs_test_123") })
        val backend = PlaygroundBackend("https://example.com", "us_tax", executor)
        val params = buildJsonObject { put("ui_mode", "elements") }

        assertThat(backend.createCheckoutSession(params)).isEqualTo("cs_test_123")
        val request = executor.requests.awaitItem()
        assertThat(request.url).isEqualTo("https://example.com/create_checkout_session")
        assertThat(request.body!!.string("stripe_version")).isEqualTo("2026-08-26.preview")
        assertThat(request.body["request_params"]).isEqualTo(params)
        executor.ensureAllEventsConsumed()
    }

    @Test
    fun `backend error message is preserved`() {
        assertThat(parseBackendError(Json, """{"error":"Invalid customer"}""".encodeToByteArray()))
            .isEqualTo("Invalid customer")
        assertThat(parseBackendError(Json, "Not JSON".encodeToByteArray())).isNull()
    }
}

private class FakePlaygroundRequestExecutor(
    var response: JsonObject,
) : PlaygroundRequestExecutor {
    val requests = Turbine<PlaygroundRequest>()

    override suspend fun execute(request: PlaygroundRequest): JsonObject {
        requests.add(request)
        return response
    }

    fun ensureAllEventsConsumed() {
        requests.ensureAllEventsConsumed()
    }
}

private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content
