package com.stripe.android.paymentsheet.example.playground.network

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import com.stripe.android.paymentsheet.example.playground.model.CreateConnectionTokenRequest
import com.stripe.android.paymentsheet.example.playground.model.CreateConnectionTokenResponse
import com.stripe.android.paymentsheet.example.playground.settings.CustomPublishableKeyDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomSecretKeyDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomStripeApiDefinition
import com.stripe.android.paymentsheet.example.playground.settings.MerchantSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal class CreateConnectionTokenRequester(
    playgroundSettings: PlaygroundSettings.Snapshot,
    applicationContext: Context,
) : BasePlaygroundRequester(playgroundSettings, applicationContext) {
    suspend fun fetch(): kotlin.Result<String> {
        val apiResponse = withContext(Dispatchers.IO) {
            Fuel.post(baseUrl + "create_connection_token")
                .jsonBody(
                    Json.encodeToString(
                        CreateConnectionTokenRequest.serializer(),
                        CreateConnectionTokenRequest(
                            merchantCountryCode = playgroundSettings[MerchantSettingsDefinition].value,
                            customStripeApi = playgroundSettings[CustomStripeApiDefinition],
                            customSecretKey = playgroundSettings[CustomSecretKeyDefinition],
                            customPublishableKey = playgroundSettings[CustomPublishableKeyDefinition],
                        )
                    )
                )
                .suspendable()
                .awaitModel(CreateConnectionTokenResponse.serializer())
        }

        return when (apiResponse) {
            is Result.Failure -> kotlin.Result.failure(apiResponse.getException())
            is Result.Success -> kotlin.Result.success(apiResponse.value.connectionToken)
        }
    }
}
