package com.stripe.android.connectsdk.example.networking

import com.github.kittinunf.fuel.core.Deserializable
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.awaitResult
import com.github.kittinunf.result.Result
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class EmbeddedComponentService {
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
        return fuel.get(EXAMPLE_BACKEND_URL + "app_info")
            .awaitModel(GetAccountsResponse.serializer())
            .get()
    }

    /**
     * Returns the client secret for the given merchant account to be used in the Stripe Connect SDK.
     * Throws a [FuelError] exception on network issues and other errors.
     */
    suspend fun fetchClientSecret(account: String): String {
        return fuel.post(EXAMPLE_BACKEND_URL + "account_session")
            .header("account", account)
            .awaitModel(FetchClientSecretResponse.serializer())
            .get()
            .clientSecret
    }

    companion object {
        private const val EXAMPLE_BACKEND_URL = "https://stripe-connect-mobile-example-v1.glitch.me/"
    }
}

@Serializable
data class FetchClientSecretResponse(
    @SerialName("client_secret")
    val clientSecret: String
)

@Serializable
data class GetAccountsResponse(
    @SerialName("publishable_key")
    val publishableKey: String,
    @SerialName("available_merchants")
    val availableMerchants: List<Merchant>
)

@Serializable
data class Merchant(
    @SerialName("merchant_id")
    val merchantId: String,
    @SerialName("display_name")
    val displayName: String
)

suspend fun <T : Any> Request.awaitModel(
    serializer: DeserializationStrategy<T>
): Result<T, FuelError> {
    val deserializer = object : Deserializable<T> {

        override fun deserialize(response: Response): T {
            println(response.toString())
            val body = response.body().asString("application/json")
            return Json.decodeFromString(serializer, body)
        }
    }

    return awaitResult(deserializer)
}
