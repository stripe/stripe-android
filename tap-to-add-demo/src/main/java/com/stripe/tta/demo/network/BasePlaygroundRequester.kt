package com.stripe.tta.demo.network

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext

internal abstract class BasePlaygroundRequester<TRequest : Any, TResponse : Any>(
    private val path: String,
    private val ioContext: CoroutineContext,
    private val requestSerializer: KSerializer<TRequest>,
    private val responseDeserializer: DeserializationStrategy<TResponse>
) {
    private val json = Json { ignoreUnknownKeys = true }

    open suspend fun fetch(
        request: TRequest,
    ): Result<TResponse> {
        return withContext(ioContext) {
            val response = Fuel.post(BASE_URL + path)
                .jsonBody(json.encodeToString(requestSerializer, request))
                .suspendable()
                .awaitModel(responseDeserializer, json)

            response.fold(
                success = { Result.success(it) },
                failure = { Result.failure(it) }
            )
        }
    }

    private companion object {
        const val BASE_URL = "https://stp-mobile-playground-backend-v7.stripedemos.com/"
    }
}
