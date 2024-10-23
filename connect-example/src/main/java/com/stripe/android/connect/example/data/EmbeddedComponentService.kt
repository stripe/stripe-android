package com.stripe.android.connect.example.data

import com.github.kittinunf.fuel.core.Deserializable
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.awaitResult
import com.github.kittinunf.result.Result
import kotlinx.coroutines.delay
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json

class EmbeddedComponentService private constructor() {

    private var exampleBackendBaseUrl: String = DEFAULT_SERVER_BASE_URL

    fun setBackendBaseUrl(url: String) {
        exampleBackendBaseUrl = url
    }

    private val fuel = FuelManager.instance
        .apply {
            // add logging
            addRequestInterceptor(RequestLogger(tag = "EmbeddedComponentService"))
            addResponseInterceptor(ResponseLogger(tag = "EmbeddedComponentService"))

            // add headers
            addRequestInterceptor(ApplicationJsonHeaderInterceptor)
            addRequestInterceptor(UserAgentHeader)
        }

    /**
     * Returns the publishable key for use in the Stripe Connect SDK as well as a list
     * of available merchants. Throws a [FuelError] exception on network issues and other errors.
     */
    suspend fun getAccounts(): GetAccountsResponse {
        delay(3_000)
        return fuel.get(exampleBackendBaseUrl + "app_info_simulate_404")
            .awaitModel(GetAccountsResponse.serializer())
            .get()
    }

    /**
     * Returns the client secret for the given merchant account to be used in the Stripe Connect SDK.
     * Throws a [FuelError] exception on network issues and other errors.
     */
    suspend fun fetchClientSecret(account: String): String {
        return fuel.post(exampleBackendBaseUrl + "account_session")
            .header("account", account)
            .awaitModel(FetchClientSecretResponse.serializer())
            .get()
            .clientSecret
    }

    companion object {
        const val DEFAULT_SERVER_BASE_URL = "https://stripe-connect-mobile-example-v1.glitch.me/"

        private var instance: EmbeddedComponentService? = null
        fun getInstance(): EmbeddedComponentService {
            return instance ?: EmbeddedComponentService().also {
                instance = it
            }
        }
    }
}

suspend fun <T : Any> Request.awaitModel(
    serializer: DeserializationStrategy<T>
): Result<T, FuelError> {
    val deserializer = object : Deserializable<T> {

        override fun deserialize(response: Response): T {
            val body = response.body().asString("application/json")
            return Json.decodeFromString(serializer, body)
        }
    }

    return awaitResult(deserializer)
}
