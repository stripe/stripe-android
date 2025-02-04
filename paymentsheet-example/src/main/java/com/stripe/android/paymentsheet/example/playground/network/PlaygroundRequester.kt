package com.stripe.android.paymentsheet.example.playground.network

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.example.Settings
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.PlaygroundState.Companion.asPlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CheckoutResponse
import com.stripe.android.paymentsheet.example.playground.settings.CustomEndpointDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import kotlinx.serialization.json.Json

internal class PlaygroundRequester(
    private val playgroundSettings: PlaygroundSettings.Snapshot,
    private val applicationContext: Context,
) {
    private val settings by lazy {
        Settings(applicationContext)
    }

    private val baseUrl: String
        get() {
            val customEndpoint = playgroundSettings[CustomEndpointDefinition]
            return customEndpoint ?: settings.playgroundBackendUrl
        }

    suspend fun fetch(): kotlin.Result<PlaygroundState> {
        val requestBody = playgroundSettings.checkoutRequest()

        val apiResponse = Fuel.post(baseUrl + "checkout")
            .jsonBody(Json.encodeToString(CheckoutRequest.serializer(), requestBody))
            .suspendable()
            .awaitModel(CheckoutResponse.serializer())
        when (apiResponse) {
            is Result.Failure -> {
                return kotlin.Result.failure(apiResponse.getException())
            }

            is Result.Success -> {
                val checkoutResponse = apiResponse.value
                println("StripeIntent ${checkoutResponse.intentClientSecret.substringBefore("_secret_")}")

                // Init PaymentConfiguration with the publishable key returned from the backend,
                // which will be used on all Stripe API calls
                PaymentConfiguration.init(
                    applicationContext,
                    checkoutResponse.publishableKey,
                )

                val customerId = checkoutResponse.customerId
                val updatedSettings = playgroundSettings.playgroundSettings()
                if (
                    playgroundSettings[CustomerSettingsDefinition] == CustomerType.NEW &&
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
