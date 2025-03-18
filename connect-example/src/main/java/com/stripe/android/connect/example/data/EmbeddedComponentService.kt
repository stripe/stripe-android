package com.stripe.android.connect.example.data

import com.github.kittinunf.fuel.core.Deserializable
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.awaitResult
import com.github.kittinunf.result.Result
import com.stripe.android.connect.example.core.Async
import com.stripe.android.connect.example.core.Fail
import com.stripe.android.connect.example.core.Loading
import com.stripe.android.connect.example.core.Success
import com.stripe.android.connect.example.core.Uninitialized
import com.stripe.android.connect.example.data.EmbeddedComponentService.Companion.DEFAULT_SERVER_BASE_URL
import com.stripe.android.core.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import javax.inject.Inject

interface EmbeddedComponentService {
    val serverBaseUrl: String
    val publishableKey: StateFlow<String?>
    val accounts: StateFlow<Async<List<Merchant>>>
    fun setBackendBaseUrl(url: String)

    /**
     * Returns the publishable key for use in the Stripe Connect SDK as well as a list
     * of available merchants. Throws a [FuelError] exception on network issues and other errors.
     */
    suspend fun getAccounts(): GetAccountsResponse

    /**
     * Returns the publishable key for use in the Stripe Connect SDK.
     * Throws a [FuelError] exception on network issues and other errors.
     */
    suspend fun loadPublishableKey(): String

    /**
     * Returns the client secret for the given merchant account to be used in the Stripe Connect SDK.
     * Throws a [FuelError] exception on network issues and other errors.
     */
    suspend fun fetchClientSecret(account: String): String

    companion object {
        const val DEFAULT_SERVER_BASE_URL = "https://stripe-connect-mobile-example-v1.glitch.me/"
    }
}

class EmbeddedComponentServiceImpl @Inject constructor(
    private val settingsService: SettingsService,
    private val logger: Logger,
) : EmbeddedComponentService {
    override var serverBaseUrl: String = settingsService.getSelectedServerBaseURL() ?: DEFAULT_SERVER_BASE_URL
        private set

    private val _publishableKey: MutableStateFlow<String?> = MutableStateFlow(null)
    override val publishableKey: StateFlow<String?> = _publishableKey

    private val _accounts: MutableStateFlow<Async<List<Merchant>>> = MutableStateFlow(Uninitialized)
    override val accounts: StateFlow<Async<List<Merchant>>> = _accounts

    override fun setBackendBaseUrl(url: String) {
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
            addRequestInterceptor(RequestLogger(tag = "EmbeddedComponentService", logger = logger))
            addResponseInterceptor(ResponseLogger(tag = "EmbeddedComponentService", logger = logger))

            // add headers
            addRequestInterceptor(ApplicationJsonHeaderInterceptor)
            addRequestInterceptor(UserAgentHeader)
        }

    /**
     * Returns the publishable key for use in the Stripe Connect SDK as well as a list
     * of available merchants. Throws a [FuelError] exception on network issues and other errors.
     */
    override suspend fun getAccounts(): GetAccountsResponse {
        return withContext(Dispatchers.IO) {
            _accounts.value = Loading()
            try {
                fuel.get(serverBaseUrl + "app_info")
                    .awaitModel(GetAccountsResponse.serializer())
                    .get()
                    .apply {
                        _publishableKey.value = publishableKey
                        _accounts.value = Success(availableMerchants)

                        // if we have no selected merchant, default to the first one
                        val firstMerchant = availableMerchants.firstOrNull()?.merchantId
                        if (settingsService.getSelectedMerchant() == null && firstMerchant != null) {
                            settingsService.setSelectedMerchant(firstMerchant)
                        }
                    }
            } catch (e: CancellationException) {
                @Suppress("RethrowCaughtException")
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                _accounts.value = Fail(e, _accounts.value())
                throw e
            }
        }
    }

    /**
     * Returns the publishable key for use in the Stripe Connect SDK.
     * Throws a [FuelError] exception on network issues and other errors.
     */
    override suspend fun loadPublishableKey(): String = getAccounts().publishableKey

    /**
     * Returns the client secret for the given merchant account to be used in the Stripe Connect SDK.
     * Throws a [FuelError] exception on network issues and other errors.
     */
    override suspend fun fetchClientSecret(account: String): String {
        return withContext(Dispatchers.IO) {
            fuel.post(serverBaseUrl + "account_session")
                .header("account", account)
                .awaitModel(FetchClientSecretResponse.serializer())
                .get()
                .clientSecret
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
