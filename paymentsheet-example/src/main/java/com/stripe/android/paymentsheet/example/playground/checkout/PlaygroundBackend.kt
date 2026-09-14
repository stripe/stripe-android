package com.stripe.android.paymentsheet.example.playground.checkout

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URI

internal interface CheckoutPlaygroundBackend {
    suspend fun fetchPublishableKey(): String
    suspend fun createCustomer(requestParams: JsonObject): String
    suspend fun attachPaymentMethod(paymentMethodId: String, customerId: String)
    suspend fun createCheckoutSession(requestParams: JsonObject): String
}

internal class PlaygroundBackend(
    baseUrl: String,
    private val merchant: String,
    private val requestExecutor: PlaygroundRequestExecutor,
) : CheckoutPlaygroundBackend {
    private val baseUrl = normalizedPlaygroundBaseUrl(baseUrl)

    constructor(baseUrl: String, merchant: String) : this(
        baseUrl = baseUrl,
        merchant = merchant,
        requestExecutor = FuelPlaygroundRequestExecutor(),
    )

    override suspend fun fetchPublishableKey(): String {
        return requestExecutor.execute(
            PlaygroundRequest(
                method = PlaygroundRequest.Method.Get,
                url = endpointUrl("publishable_key"),
                queryParameters = listOf("merchant" to merchant),
                body = null,
            )
        ).requiredString("publishable_key")
    }

    override suspend fun createCustomer(requestParams: JsonObject): String {
        return post(
            endpoint = "create_customer",
            requestParams = requestParams,
            stripeVersion = null,
            additionalFields = JsonObject(emptyMap()),
        ).requiredString("id")
    }

    override suspend fun attachPaymentMethod(paymentMethodId: String, customerId: String) {
        post(
            endpoint = "attach_payment_method",
            requestParams = buildJsonObject { put("customer", customerId) },
            stripeVersion = null,
            additionalFields = buildJsonObject { put("payment_method_id", paymentMethodId) },
        )
    }

    override suspend fun createCheckoutSession(requestParams: JsonObject): String {
        return post(
            endpoint = "create_checkout_session",
            requestParams = requestParams,
            stripeVersion = CHECKOUT_API_VERSION,
            additionalFields = JsonObject(emptyMap()),
        ).requiredString("client_secret")
    }

    private suspend fun post(
        endpoint: String,
        requestParams: JsonObject,
        stripeVersion: String?,
        additionalFields: JsonObject,
    ): JsonObject {
        val body = buildJsonObject {
            put("merchant", merchant)
            put("request_params", requestParams)
            stripeVersion?.let { put("stripe_version", it) }
            additionalFields.forEach { (key, value) -> put(key, value) }
        }
        return requestExecutor.execute(
            PlaygroundRequest(
                method = PlaygroundRequest.Method.Post,
                url = endpointUrl(endpoint),
                queryParameters = emptyList(),
                body = body,
            )
        )
    }

    private fun endpointUrl(endpoint: String): String = "$baseUrl/$endpoint"

    private fun JsonObject.requiredString(key: String): String {
        return this[key]?.jsonPrimitive?.content
            ?: throw IllegalStateException("Backend response is missing '$key'")
    }

    private companion object {
        const val CHECKOUT_API_VERSION = "2026-08-26.preview"
    }
}

internal data class PlaygroundRequest(
    val method: Method,
    val url: String,
    val queryParameters: List<Pair<String, String>>,
    val body: JsonObject?,
) {
    enum class Method { Get, Post }
}

internal fun interface PlaygroundRequestExecutor {
    suspend fun execute(request: PlaygroundRequest): JsonObject
}

private class FuelPlaygroundRequestExecutor : PlaygroundRequestExecutor {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun execute(request: PlaygroundRequest): JsonObject = withContext(Dispatchers.IO) {
        val fuelRequest = when (request.method) {
            PlaygroundRequest.Method.Get -> Fuel.get(request.url, request.queryParameters)
            PlaygroundRequest.Method.Post -> Fuel.post(request.url).jsonBody(requireNotNull(request.body).toString())
        }
        when (val response = fuelRequest.suspendable().awaitModel(JsonObject.serializer(), json)) {
            is Result.Success -> response.value
            is Result.Failure -> throw response.getException().withBackendMessage(json)
        }
    }
}

internal fun normalizedPlaygroundBaseUrl(value: String): String {
    val normalized = value.trim().trimEnd('/')
    val uri = runCatching { URI(normalized) }.getOrNull()
    require(uri != null && uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()) {
        "Must be a valid base URL"
    }
    require(uri.query == null && uri.fragment == null) { "Backend URL must not contain a query or fragment" }
    require(LEGACY_ENDPOINTS.none { uri.path.trimEnd('/').endsWith(it) }) {
        "Enter the backend base URL, not a Checkout Session endpoint"
    }
    return normalized
}

internal fun FuelError.withBackendMessage(json: Json): Throwable {
    val message = parseBackendError(json, errorData)
    return message?.let { IllegalStateException(it, this) } ?: this
}

internal fun parseBackendError(json: Json, errorData: ByteArray): String? = runCatching {
    json.decodeFromString<BackendErrorResponse>(errorData.decodeToString()).error
}.getOrNull()

@kotlinx.serialization.Serializable
private data class BackendErrorResponse(val error: String)

private val LEGACY_ENDPOINTS = listOf("/checkout_session", "/create_checkout_session")
