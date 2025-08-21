package com.stripe.android.crypto.onramp.example.network

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Deserializable
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.awaitResult
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import com.github.kittinunf.result.Result as ApiResult

class TestBackendRepository {

    private val baseUrl = "https://crypto-onramp-example.stripedemos.com"

    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun createAuthIntent(
        email: String
    ): ApiResult<CreateAuthIntentResponse, FuelError> {
        return withContext(Dispatchers.IO) {
            val request = CreateAuthIntentRequest(email = email)
            val requestBody = json.encodeToString(CreateAuthIntentRequest.serializer(), request)

            Fuel.post("$baseUrl/auth_intent/create")
                .jsonBody(requestBody)
                .suspendable()
                .awaitModel(CreateAuthIntentResponse.serializer(), json)
        }
    }

    suspend fun createOnrampSession(
        paymentToken: String,
        walletAddress: String,
        cryptoCustomerId: String,
        authToken: String,
        destinationNetwork: String = "ethereum",
        sourceAmount: Double = 10.0,
        sourceCurrency: String = "usd",
        destinationCurrency: String = "eth",
        customerIpAddress: String = "127.0.0.1"
    ): ApiResult<OnrampSessionResponse, FuelError> {
        return withContext(Dispatchers.IO) {
            val request = CreateOnrampSessionRequest(
                uiMode = "headless",
                paymentToken = paymentToken,
                sourceAmount = sourceAmount,
                sourceCurrency = sourceCurrency,
                destinationCurrency = destinationCurrency,
                destinationNetwork = destinationNetwork,
                walletAddress = walletAddress,
                cryptoCustomerId = cryptoCustomerId,
                customerIpAddress = customerIpAddress
            )

            val requestBody = json.encodeToString(CreateOnrampSessionRequest.serializer(), request)

            Fuel.post("$baseUrl/create_onramp_session")
                .timeout(SESSION_CREATION_TIMEOUT)
                .timeoutRead(SESSION_CREATION_TIMEOUT)
                .header("Authorization", "Bearer $authToken")
                .jsonBody(requestBody)
                .suspendable()
                .awaitModel(OnrampSessionResponse.serializer(), json)
        }
    }

    @Suppress("unused")
    suspend fun checkout(
        cosId: String,
        authToken: String
    ): ApiResult<OnrampSessionResponse, FuelError> {
        return withContext(Dispatchers.IO) {
            val request = CheckoutRequest(cosId = cosId)
            val requestBody = json.encodeToString(CheckoutRequest.serializer(), request)

            Fuel.post("$baseUrl/checkout")
                .header("Authorization", "Bearer $authToken")
                .jsonBody(requestBody)
                .suspendable()
                .awaitModel(OnrampSessionResponse.serializer(), json)
        }
    }
}

/**
 * Awaits the [ApiResult] and deserializes it into the desired type [T].
 */
suspend fun <T : Any> Request.awaitModel(
    serializer: DeserializationStrategy<T>,
    json: Json = Json,
): ApiResult<T, FuelError> {
    val deserializer = object : Deserializable<T> {
        override fun deserialize(response: Response): T {
            val body = response.body().asString("application/json")
            return json.decodeFromString(serializer, body)
        }
    }
    return awaitResult(deserializer)
}

private const val SESSION_CREATION_TIMEOUT = 60000 // 60 seconds
