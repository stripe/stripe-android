package com.stripe.tta.demo.network

import com.github.kittinunf.fuel.core.Deserializable
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.awaitResult
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json

/**
 * Awaits the result and deserializes it into the desired type [T].
 */
suspend fun <T : Any> Request.awaitModel(
    serializer: DeserializationStrategy<T>,
    json: Json = Json,
): com.github.kittinunf.result.Result<T, FuelError> {
    val deserializer = object : Deserializable<T> {

        override fun deserialize(response: Response): T {
            val body = response.body().asString("application/json")
            return json.decodeFromString(serializer, body)
        }
    }

    return awaitResult(deserializer)
}
