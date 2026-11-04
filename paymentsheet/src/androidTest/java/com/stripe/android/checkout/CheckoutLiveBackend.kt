package com.stripe.android.checkout

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class CheckoutLiveBackend(baseUrl: String) {
    private val baseUrl = baseUrl.trimEnd('/')

    suspend fun fetchPublishableKey(): String {
        return execute(
            endpoint = "publishable_key",
            request = Fuel.get("$baseUrl/publishable_key", listOf("merchant" to "US")),
        ).requiredString("publishable_key")
    }

    suspend fun createCheckoutSession(requestParams: JsonObject): String {
        val body = buildJsonObject {
            put("merchant", "US")
            put("stripe_version", "2026-08-26.preview")
            put("request_params", requestParams)
        }
        return execute(
            endpoint = "create_checkout_session",
            request = Fuel.post("$baseUrl/create_checkout_session").jsonBody(body.toString()),
        ).requiredString("client_secret")
    }

    private suspend fun execute(endpoint: String, request: Request): JsonObject = withContext(Dispatchers.IO) {
        val (_, response, result) = request
            .timeout(REQUEST_TIMEOUT_MILLIS)
            .timeoutRead(REQUEST_TIMEOUT_MILLIS)
            .responseString()
        val body = when (result) {
            is Result.Success -> result.value
            is Result.Failure -> error(
                "Playground '$endpoint' failed: HTTP ${response.statusCode}, " +
                    result.error.exception.javaClass.simpleName
            )
        }
        try {
            Json.decodeFromString<JsonObject>(body)
        } catch (_: SerializationException) {
            error("Playground '$endpoint' returned invalid JSON")
        }
    }

    private fun JsonObject.requiredString(key: String): String {
        return (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }
            ?: error("Playground response is missing '$key'")
    }

    private companion object {
        const val REQUEST_TIMEOUT_MILLIS = 30_000
    }
}
