package com.stripe.android.identity.networked

import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.OutputStream

private val json = Json { ignoreUnknownKeys = true }

/**
 * Sends [request] once and decodes its body with [deserializer]. Single-use tokens must never be replayed, so the
 * request opts out of the client's retries. Errors keep only the status and Stripe error type and code: URLs, bodies
 * and backend messages can contain document IDs, credentials or submitted identity data.
 */
internal suspend fun <T> StripeNetworkClient.postNetworkedIdentity(
    request: ApiRequest,
    deserializer: DeserializationStrategy<T>,
): Result<T> {
    val response = try {
        executeRequest(NetworkedIdentityRequest(request))
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        return Result.failure(APIConnectionException(message = "Networked Identity request failed."))
    }
    return try {
        val body = requireNotNull(response.body)
        if (response.isError) {
            val error = StripeErrorJsonParser().parse(JSONObject(body))
            Result.failure(
                APIException(
                    stripeError = StripeError(type = error.type, code = error.code),
                    requestId = response.requestId?.value,
                    statusCode = response.code,
                    message = "Networked Identity request failed.",
                )
            )
        } else {
            Result.success(json.decodeFromString(deserializer, body))
        }
    } catch (_: Exception) {
        Result.failure(
            APIException(
                requestId = response.requestId?.value,
                statusCode = response.code,
                message = "Networked Identity response was invalid.",
            )
        )
    }
}

private class NetworkedIdentityRequest(private val request: ApiRequest) : StripeRequest() {
    override val method: Method = request.method
    override val mimeType: MimeType = request.mimeType
    override val retryResponseCodes: Iterable<Int> = emptyList()
    override val url: String = request.url
    override val headers: Map<String, String> = request.headers
    override var postHeaders: Map<String, String>? = request.postHeaders

    override fun writePostBody(outputStream: OutputStream) = request.writePostBody(outputStream)

    override fun toString(): String = "NetworkedIdentityRequest(POST, [redacted])"
}
