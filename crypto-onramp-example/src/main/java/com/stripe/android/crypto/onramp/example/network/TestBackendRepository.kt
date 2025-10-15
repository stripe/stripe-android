package com.stripe.android.crypto.onramp.example.network

import com.github.kittinunf.fuel.core.Deserializable
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.awaitResult
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.interceptors.LogRequestAsCurlInterceptor
import com.github.kittinunf.fuel.core.interceptors.LogRequestInterceptor
import com.github.kittinunf.fuel.core.interceptors.LogResponseInterceptor
import com.github.kittinunf.fuel.core.requests.suspendable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import com.github.kittinunf.result.Result as ApiResult

class TestBackendRepository {

    private val baseUrl = "https://crypto-onramp-example.stripedemos.com"
    private val baseUrlV1 = "https://crypto-onramp-example.stripedemos.com/v1"

    private val manager = FuelManager()
        .addRequestInterceptor(LogRequestInterceptor)
        .addResponseInterceptor(LogResponseInterceptor)
        .addRequestInterceptor(LogRequestAsCurlInterceptor)

    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun createAuthIntent(
        email: String,
        oauthScopes: String,
    ): ApiResult<CreateAuthIntentResponse, FuelError> {
        return withContext(Dispatchers.IO) {
            val request = CreateAuthIntentRequest(
                email = email,
                oauthScopes = oauthScopes,
            )
            val requestBody = json.encodeToString(CreateAuthIntentRequest.serializer(), request)

            manager.post("$baseUrl/auth_intent/create")
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

            manager.post("$baseUrl/create_onramp_session")
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

            manager.post("$baseUrl/checkout")
                .timeout(SESSION_CREATION_TIMEOUT)
                .timeoutRead(SESSION_CREATION_TIMEOUT)
                .header("Authorization", "Bearer $authToken")
                .jsonBody(requestBody)
                .suspendable()
                .awaitModel(OnrampSessionResponse.serializer(), json)
        }
    }

    suspend fun signUp(
        email: String,
        password: String,
        livemode: Boolean
    ): ApiResult<LoginSignUpResponse, FuelError> {
        return withContext(Dispatchers.IO) {
            val request = LoginSignUpRequest(
                email = email,
                password = password,
                livemode = livemode
            )

            val requestBody = json.encodeToString(LoginSignUpRequest.serializer(), request)

            manager.post("$baseUrlV1/auth/signup")
                .timeout(SESSION_CREATION_TIMEOUT)
                .timeoutRead(SESSION_CREATION_TIMEOUT)
                .jsonBody(requestBody)
                .suspendable()
                .awaitModel(LoginSignUpResponse.serializer(), json)
        }
    }

    suspend fun logIn(
        email: String,
        password: String,
        livemode: Boolean,
    ): ApiResult<LoginSignUpResponse, FuelError> {
        return withContext(Dispatchers.IO) {
            val request = LoginSignUpRequest(
                email = email,
                password = password,
                livemode = livemode
            )

            val requestBody = json.encodeToString(LoginSignUpRequest.serializer(), request)

            manager.post("$baseUrlV1/auth/login")
                .timeout(SESSION_CREATION_TIMEOUT)
                .timeoutRead(SESSION_CREATION_TIMEOUT)
                .jsonBody(requestBody)
                .suspendable()
                .awaitModel(LoginSignUpResponse.serializer(), json)
        }
    }

    suspend fun create(
        oauthScopes: String,
        tokenWithoutLAI: String
    ): ApiResult<AuthCreateResponse, FuelError> {
        return withContext(Dispatchers.IO) {
            val request = AuthCreateRequest(
                oauthScopes = oauthScopes
            )

            val requestBody = json.encodeToString(AuthCreateRequest.serializer(), request)

            manager.post("$baseUrlV1/auth/create")
                .timeout(SESSION_CREATION_TIMEOUT)
                .timeoutRead(SESSION_CREATION_TIMEOUT)
                .header("Authorization", "Bearer $tokenWithoutLAI")
                .jsonBody(requestBody)
                .suspendable()
                .awaitModel(AuthCreateResponse.serializer(), json)
        }
    }

    suspend fun saveUser(
        cryptoCustomerId: String,
        tokenWithLAI: String
    ): ApiResult<SaveUserResponse, FuelError> {
        return withContext(Dispatchers.IO) {
            val request = SaveUserRequest(
                cryptoCustomerId = cryptoCustomerId
            )

            val requestBody = json.encodeToString(SaveUserRequest.serializer(), request)

            manager.post("$baseUrlV1/auth/save_user")
                .timeout(SESSION_CREATION_TIMEOUT)
                .timeoutRead(SESSION_CREATION_TIMEOUT)
                .header("Authorization", "Bearer $tokenWithLAI")
                .jsonBody(requestBody)
                .suspendable()
                .awaitModel(SaveUserResponse.serializer(), json)
        }
    }

    suspend fun createLinkAuthToken(
        tokenWithLAI: String
    ): ApiResult<CreateLinkAuthTokenResponse, FuelError> {
        return withContext(Dispatchers.IO) {
            manager.post("$baseUrlV1/auth/create_link_auth_token")
                .timeout(SESSION_CREATION_TIMEOUT)
                .timeoutRead(SESSION_CREATION_TIMEOUT)
                .header("Authorization", "Bearer $tokenWithLAI")
                .suspendable()
                .awaitModel(CreateLinkAuthTokenResponse.serializer(), json)
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
