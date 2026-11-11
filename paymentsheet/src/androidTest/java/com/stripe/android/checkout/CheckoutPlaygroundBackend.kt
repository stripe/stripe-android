package com.stripe.android.checkout

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Deserializable
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.awaitResult
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONObject

internal class CheckoutPlaygroundBackend(
    baseUrl: String,
    private val merchant: String,
) {
    private val baseUrl = baseUrl.trimEnd('/')

    suspend fun fetchPublishableKey(): kotlin.Result<String> {
        return execute(
            request = Fuel.get("$baseUrl/publishable_key", listOf("merchant" to merchant)),
            responseKey = "publishable_key",
        )
    }

    suspend fun createCheckoutSession(requestParams: JsonObject): kotlin.Result<String> {
        val body = buildJsonObject {
            put("merchant", merchant)
            put("stripe_version", "2026-08-26.preview")
            put("request_params", requestParams)
        }
        return execute(
            request = Fuel.post("$baseUrl/create_checkout_session").jsonBody(body.toString()),
            responseKey = "client_secret",
        )
    }

    suspend fun createCustomer(requestParams: JsonObject): kotlin.Result<String> {
        val body = buildJsonObject {
            put("merchant", merchant)
            put("request_params", requestParams)
        }
        return execute(
            request = Fuel.post("$baseUrl/create_customer").jsonBody(body.toString()),
            responseKey = "id",
        )
    }

    private suspend fun execute(request: Request, responseKey: String): kotlin.Result<String> =
        withContext(Dispatchers.IO) {
            val deserializer = object : Deserializable<String> {
                override fun deserialize(response: Response): String {
                    val body = response.body().asString("application/json")
                    return JSONObject(body).getString(responseKey)
                }
            }
            when (val result = request.awaitResult(deserializer)) {
                is Result.Success -> kotlin.Result.success(result.value)
                is Result.Failure -> kotlin.Result.failure(result.getException())
            }
        }
}
