package com.stripe.android.paymentsheet.example.playground.checkout

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.example.Settings
import com.stripe.android.paymentsheet.example.playground.model.CheckoutResponse
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal class CheckoutControllerExampleBackendRepository(
    private val applicationContext: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val defaultBackendUrl = Settings(applicationContext).playgroundBackendUrl

    suspend fun fetchCheckoutSession(
        request: CheckoutControllerExampleRequest,
        backendUrl: String?,
    ): kotlin.Result<CheckoutControllerExampleBackendResponse> {
        val apiResponse = withContext(Dispatchers.IO) {
            Fuel.post(checkoutSessionUrl(defaultBackendUrl, backendUrl, request.endpoint))
                .jsonBody(request.body.toString())
                .suspendable()
                .awaitModel(CheckoutResponse.serializer(), json)
        }

        return when (apiResponse) {
            is Result.Failure -> {
                kotlin.Result.failure(apiResponse.getException().withBackendMessage(json))
            }
            is Result.Success -> {
                val response = apiResponse.value

                withContext(Dispatchers.IO) {
                    PaymentConfiguration.init(applicationContext, response.publishableKey)
                }

                val clientSecret = response.checkoutSessionClientSecret
                    ?: return kotlin.Result.failure(
                        IllegalStateException("No checkout session client secret in response")
                    )

                kotlin.Result.success(
                    CheckoutControllerExampleBackendResponse(
                        clientSecret = clientSecret,
                        customerId = response.customerId,
                    )
                )
            }
        }
    }
}

internal data class CheckoutControllerExampleBackendResponse(
    val clientSecret: String,
    val customerId: String?,
)

internal fun checkoutSessionUrl(
    defaultBackendUrl: String,
    customBackendUrl: String?,
    endpoint: String,
): String {
    return (customBackendUrl ?: defaultBackendUrl) + endpoint
}

internal fun FuelError.withBackendMessage(json: Json): Throwable {
    val message = parseCheckoutSessionError(json, errorData)

    return message?.let { IllegalStateException(it, this) } ?: this
}

internal fun parseCheckoutSessionError(json: Json, errorData: ByteArray): String? {
    return runCatching {
        json.decodeFromString<CheckoutSessionErrorResponse>(errorData.decodeToString()).error
    }.getOrNull()
}

@kotlinx.serialization.Serializable
private data class CheckoutSessionErrorResponse(
    val error: String,
)
