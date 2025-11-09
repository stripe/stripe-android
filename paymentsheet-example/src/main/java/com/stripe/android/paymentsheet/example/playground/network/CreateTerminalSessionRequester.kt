package com.stripe.android.paymentsheet.example.playground.network

import android.content.Context
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import com.stripe.android.paymentsheet.example.Settings
import com.stripe.android.paymentsheet.example.playground.model.CreateTerminalSessionRequest
import com.stripe.android.paymentsheet.example.playground.model.CreateTerminalSessionResponse
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomEndpointDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal class CreateTerminalSessionRequester(
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

    suspend fun fetch(): kotlin.Result<CreateTerminalSessionResponse> {
        val apiResponse = withContext(Dispatchers.IO) {
            Fuel.post(baseUrl + "create_terminal_session")
                .jsonBody(
                    Json.encodeToString(
                        CreateTerminalSessionRequest.serializer(),
                        CreateTerminalSessionRequest(
                            merchantCountryCode = playgroundSettings[CountrySettingsDefinition].value,
                        )
                    )
                )
                .suspendable()
                .awaitModel(CreateTerminalSessionResponse.serializer())
        }

        return when (apiResponse) {
            is Result.Failure -> kotlin.Result.failure(apiResponse.getException())
            is Result.Success -> kotlin.Result.success(apiResponse.value)
        }
    }
}
