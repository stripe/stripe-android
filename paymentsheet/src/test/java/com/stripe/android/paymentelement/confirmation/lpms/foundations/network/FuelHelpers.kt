package com.stripe.android.paymentelement.confirmation.lpms.foundations.network

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Deserializable
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.awaitResult
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

internal suspend fun <T : Any, R : Any> executeFuelPostRequest(
    url: String,
    request: T,
    requestSerializer: SerializationStrategy<T>,
    responseDeserializer: DeserializationStrategy<R>,
): kotlin.Result<R> {
    return Fuel.post(url)
        .jsonBody(
            Json.encodeToString(
                requestSerializer,
                request,
            )
        )
        .execute(responseDeserializer)
}

internal suspend fun <R : Any> executeFuelGetRequest(
    url: String,
    parameters: List<Pair<String, Any?>>,
    responseDeserializer: DeserializationStrategy<R>
): kotlin.Result<R> {
    return Fuel.get(url)
        .parameters(parameters)
        .execute(responseDeserializer)
}

private suspend fun <R : Any> Request.execute(
    responseDeserializer: DeserializationStrategy<R>
): kotlin.Result<R> {
    return suspendable()
        .awaitModel(responseDeserializer)
        .fold(
            success = { response ->
                kotlin.Result.success(response)
            },
            failure = { error ->
                kotlin.Result.failure(error)
            }
        )
}

private fun Request.parameters(
    parameters: List<Pair<String, Any?>>
) = apply {
    this.parameters = parameters
}

internal suspend fun <T : Any> Request.awaitModel(
    serializer: DeserializationStrategy<T>
): Result<T, FuelError> {
    val deserializer = object : Deserializable<T> {

        override fun deserialize(response: Response): T {
            val body = response.body().asString(CONTENT_TYPE)
            return Json.decodeFromString(serializer, body)
        }
    }

    return awaitResult(deserializer)
}

private const val CONTENT_TYPE = "application/json"
