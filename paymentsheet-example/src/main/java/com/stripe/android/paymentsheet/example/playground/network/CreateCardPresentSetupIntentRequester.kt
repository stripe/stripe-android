package com.stripe.android.paymentsheet.example.playground.network

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import com.stripe.android.paymentsheet.example.playground.model.CreateCardPresentSetupIntentRequest
import com.stripe.android.paymentsheet.example.playground.model.CreateCardPresentSetupIntentResponse
import com.stripe.android.paymentsheet.example.playground.settings.CustomPublishableKeyDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomSecretKeyDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomStripeApiDefinition
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal class CreateCardPresentSetupIntentRequester(
    playgroundSettings: PlaygroundSettings.Snapshot,
    applicationContext: Context,
) : BasePlaygroundRequester(playgroundSettings, applicationContext) {
    suspend fun fetch(customerId: String): kotlin.Result<String> {
        val apiResponse = withContext(Dispatchers.IO) {
            Fuel.post(baseUrl + "create_setup_intent")
                .jsonBody(
                    Json.encodeToString(
                        CreateCardPresentSetupIntentRequest.serializer(),
                        CreateCardPresentSetupIntentRequest(
                            customerId = customerId,
                            merchantCountryCode = playgroundSettings[MerchantSettingsDefinition].value,
                            paymentMethodTypes = listOf("card_present"),
                            customStripeApi = playgroundSettings[CustomStripeApiDefinition],
                            customSecretKey = playgroundSettings[CustomSecretKeyDefinition],
                            customPublishableKey = playgroundSettings[CustomPublishableKeyDefinition],
                        )
                    )
                )
                .suspendable()
                .awaitModel(CreateCardPresentSetupIntentResponse.serializer())
        }

        return when (apiResponse) {
            is Result.Failure -> kotlin.Result.failure(apiResponse.getException())
            is Result.Success -> kotlin.Result.success(apiResponse.value.clientSecret)
        }
    }
}
