package com.stripe.android.paymentsheet.example.playground.checkout

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.example.Settings
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
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

    suspend fun fetchCheckoutSessionClientSecret(): kotlin.Result<String> {
        val requestBody = CheckoutRequest.Builder()
            .initialization("checkout_session")
            .useCheckoutSession(true)
            .mode("payment")
            .currency("usd")
            .amount(5000)
            .customer("new")
            .build()

        val apiResponse = withContext(Dispatchers.IO) {
            Fuel.post(backendUrl + "checkout")
                .jsonBody(json.encodeToString(CheckoutRequest.serializer(), requestBody))
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
