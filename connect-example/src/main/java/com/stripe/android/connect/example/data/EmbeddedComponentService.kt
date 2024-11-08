package com.stripe.android.connect.example.data

import com.github.kittinunf.fuel.core.Deserializable
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.awaitResult
import com.github.kittinunf.result.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import javax.inject.Inject

class EmbeddedComponentService @Inject constructor(
    private val settingsService: SettingsService,
) {
    var serverBaseUrl: String = settingsService.getSelectedServerBaseURL() ?: DEFAULT_SERVER_BASE_URL
        private set

    private val _publishableKey: MutableStateFlow<String?> = MutableStateFlow(null)
    val publishableKey: StateFlow<String?> = _publishableKey

    private val _accounts: MutableStateFlow<List<Merchant>?> = MutableStateFlow(null)
    val accounts: StateFlow<List<Merchant>?> = _accounts

    fun setBackendBaseUrl(url: String) {
        serverBaseUrl = if (!url.endsWith("/")) {
            "$url/"
        } else {
            url
        }
        settingsService.setSelectedServerBaseURL(url)
    }

    private val fuel = FuelManager.instance
        .apply {
            // Set the timeout to 30 seconds (longer than standard due to
            // the Glitch server sleeping after 5 minutes of inactivity)
            timeoutInMillisecond = 30_000
            timeoutReadInMillisecond = 30_000

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
        return fuel.get(serverBaseUrl + "app_info")
            .awaitModel(GetAccountsResponse.serializer())
            .get()
            .apply {
                _publishableKey.value = publishableKey
                _accounts.value = availableMerchants
            }
    }

    /**
     * Returns the client secret for the given merchant account to be used in the Stripe Connect SDK.
     * Throws a [FuelError] exception on network issues and other errors.
     */
    suspend fun fetchClientSecret(account: String): String {
        return fuel.post(serverBaseUrl + "account_session")
            .header("account", account)
            .awaitModel(FetchClientSecretResponse.serializer())
            .get()
            .clientSecret
    }

    companion object {
        const val DEFAULT_SERVER_BASE_URL = "https://stripe-connect-mobile-example-v1.glitch.me/"
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
