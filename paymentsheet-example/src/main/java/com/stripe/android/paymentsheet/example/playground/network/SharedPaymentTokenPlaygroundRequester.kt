package com.stripe.android.paymentsheet.example.playground.network

import android.content.Context
import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.SharedPaymentTokenCreateIntentRequest
import com.stripe.android.paymentsheet.example.playground.model.SharedPaymentTokenCreateIntentResponse
import com.stripe.android.paymentsheet.example.playground.model.SharedPaymentTokenCreateSessionRequest
import com.stripe.android.paymentsheet.example.playground.model.SharedPaymentTokenCreateSessionResponse
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal class SharedPaymentTokenPlaygroundRequester(
    private val playgroundSnapshot: PlaygroundSettings.Snapshot,
    private val applicationContext: Context,
) {
    private val json = Json { explicitNulls = false }

    suspend fun session(): Result<PlaygroundState> {
        val customerId = playgroundSnapshot[CustomerSettingsDefinition].run {
            when (this) {
                is CustomerType.Existing -> customerId
                else -> null
            }
        }

        val request = SharedPaymentTokenCreateSessionRequest(
            customerId = customerId,
            isMobile = true,
        )

        val apiResponse = withContext(Dispatchers.IO) {
            Fuel.post(BASE_URL + CUSTOMER_PATH)
                .jsonBody(json.encodeToString(SharedPaymentTokenCreateSessionRequest.serializer(), request))
                .suspendable()
                .awaitModel(SharedPaymentTokenCreateSessionResponse.serializer(), json)
        }

        return when (apiResponse) {
            is com.github.kittinunf.result.Result.Failure -> Result.failure(apiResponse.getException())
            is com.github.kittinunf.result.Result.Success -> {
                val sharedPaymentTokenCreateSessionResponse = apiResponse.value

                // Init PaymentConfiguration with the publishable key returned from the backend,
                // which will be used on all Stripe API calls
                withContext(Dispatchers.IO) {
                    PaymentConfiguration.init(
                        applicationContext,
                        PUBLISHABLE_KEY,
                    )
                }

                val retrievedCustomerId = sharedPaymentTokenCreateSessionResponse.customerId
                val updatedSettings = playgroundSnapshot.playgroundSettings()

                if (
                    playgroundSnapshot[CustomerSettingsDefinition] == CustomerType.NEW
                ) {
                    updatedSettings[CustomerSettingsDefinition] = CustomerType.Existing(retrievedCustomerId)
                }

                val updatedState = PlaygroundState.SharedPaymentToken(
                    snapshot = updatedSettings.snapshot(),
                    customerId = retrievedCustomerId,
                    customerSessionClientSecret = sharedPaymentTokenCreateSessionResponse.customerSessionClientSecret
                )

                return Result.success(updatedState)
            }
        }
    }

    suspend fun spt(
        paymentMethod: PaymentMethod
    ): Result<String?> {
        val customerId = playgroundSnapshot[CustomerSettingsDefinition].run {
            when (this) {
                is CustomerType.Existing -> customerId
                else -> throw IllegalStateException("Cannot create SPT without en existing customer!")
            }
        }

        val request = SharedPaymentTokenCreateIntentRequest(
            customerId = customerId,
            paymentMethod = paymentMethod.id ?: throw IllegalStateException(
                "No payment method ID was found when creating SPT!"
            ),
            shipping = null,
        )

        val apiResponse = withContext(Dispatchers.IO) {
            Fuel.post(BASE_URL + CREATE_INTENT_PATH)
                .jsonBody(json.encodeToString(SharedPaymentTokenCreateIntentRequest.serializer(), request))
                .suspendable()
                .awaitModel(SharedPaymentTokenCreateIntentResponse.serializer(), json)
        }

        return when (apiResponse) {
            is com.github.kittinunf.result.Result.Failure -> Result.failure(apiResponse.getException())
            is com.github.kittinunf.result.Result.Success -> {
                Log.d("PI", apiResponse.value.paymentIntentId)
                Result.success(apiResponse.value.nextActionValue)
            }
        }
    }

    companion object {
        private const val BASE_URL = "https://rough-lying-carriage.glitch.me/"

        private const val CREATE_INTENT_PATH = "create-intent"
        private const val CUSTOMER_PATH = "customer"

        const val PUBLISHABLE_KEY = "pk_test_51LsBpsAoVfWZ5CNZi82L5ALZB9C89AyblMIWBHERPJRvSTaLYjaTsj" +
            "T7hMeVRuXzTIc9VkkiZQ59KqXqVxYL7Rn600Homq7UPk"
    }
}
