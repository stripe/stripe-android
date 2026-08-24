package com.stripe.android.paymentsheet.example.playground.checkout

import android.content.Context
import com.github.kittinunf.fuel.Fuel
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
    private val backendUrl = Settings(applicationContext).playgroundBackendUrl

    suspend fun fetchCheckoutSessionClientSecret(
        scenario: CheckoutControllerExampleScenario,
    ): kotlin.Result<String> {
        val request = CheckoutControllerExampleRequestFactory.create(scenario)

        val apiResponse = withContext(Dispatchers.IO) {
            Fuel.post(backendUrl + request.endpoint)
                .jsonBody(request.body.toString())
                .suspendable()
                .awaitModel(CheckoutResponse.serializer(), json)
        }

        return when (apiResponse) {
            is Result.Failure -> {
                kotlin.Result.failure(apiResponse.getException())
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

                kotlin.Result.success(clientSecret)
            }
        }
    }

}
