package com.stripe.android.paymentsheet.example.playground.network

import android.content.Context
import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.PlaygroundState.Companion.asPlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CheckoutResponse
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal class PlaygroundRequester(
    playgroundSettings: PlaygroundSettings.Snapshot,
    applicationContext: Context,
) : BasePlaygroundRequester(playgroundSettings, applicationContext) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(): kotlin.Result<PlaygroundState> {
        playgroundSettings.setValues()
        val requestBody = playgroundSettings.checkoutRequest()

        val apiResponse = withContext(Dispatchers.IO) {
            Fuel.post(baseUrl + "checkout")
                .jsonBody(json.encodeToString(CheckoutRequest.serializer(), requestBody))
                .suspendable()
                .awaitModel(CheckoutResponse.serializer(), json)
        }
        when (apiResponse) {
            is Result.Failure -> {
                Log.e("PaymentSheetPlaygroundViewModel", "Failed to fetch playground", apiResponse.getException())
                return kotlin.Result.failure(apiResponse.getException())
            }

            is Result.Success -> {
                val checkoutResponse = apiResponse.value
                println("StripeIntent ${checkoutResponse.clientSecret.substringBefore("_secret_")}")

                // Init PaymentConfiguration with the publishable key returned from the backend,
                // which will be used on all Stripe API calls
                withContext(Dispatchers.IO) {
                    PaymentConfiguration.init(
                        applicationContext,
                        checkoutResponse.publishableKey,
                    )
                }

                val customerId = checkoutResponse.customerId
                val updatedSettings = playgroundSettings.playgroundSettings()
                val currentCustomerType = playgroundSettings[CustomerSettingsDefinition]
                if (
                    (currentCustomerType == CustomerType.NEW || currentCustomerType == CustomerType.RETURNING) &&
                    customerId != null
                ) {
                    println("Customer $customerId")
                    updatedSettings[CustomerSettingsDefinition] = CustomerType.Existing(customerId)
                }
                val updatedState = checkoutResponse.asPlaygroundState(
                    snapshot = updatedSettings.snapshot(),
                    defaultEndpoint = settings.playgroundBackendUrl
                )
                return kotlin.Result.success(updatedState)
            }
        }
    }
}
