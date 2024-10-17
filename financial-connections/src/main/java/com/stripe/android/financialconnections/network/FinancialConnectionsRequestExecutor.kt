package com.stripe.android.financialconnections.network

import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.exception.PermissionException
import com.stripe.android.core.exception.RateLimitException
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.HTTP_TOO_MANY_REQUESTS
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.networking.responseJson
import com.stripe.android.financialconnections.analytics.FinancialConnectionsResponseEventEmitter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import javax.inject.Inject

internal class FinancialConnectionsRequestExecutor @Inject constructor(
    private val stripeNetworkClient: StripeNetworkClient,
    private val eventEmitter: FinancialConnectionsResponseEventEmitter,
    private val json: Json,
    private val logger: Logger,
) {

    @Throws(
        InvalidRequestException::class,
        AuthenticationException::class,
        APIException::class
    )
    suspend fun execute(request: StripeRequest): String {
        return executeInternal(request) { body ->
            body
        }
    }

    @Throws(
        InvalidRequestException::class,
        AuthenticationException::class,
        APIException::class
    )
    suspend fun <Response> execute(
        request: StripeRequest,
        responseSerializer: KSerializer<Response>
    ): Response {
        return executeInternal(request) { body ->
            json.decodeFromString(responseSerializer, body)
        }
    }

    @Throws(
        InvalidRequestException::class,
        AuthenticationException::class,
        APIException::class
    )
    private suspend fun <Response> executeInternal(
        request: StripeRequest,
        decodeResponse: (String) -> Response,
    ): Response = runCatching {
        logger.debug("Executing ${request.method.code} request to ${request.url}")
        stripeNetworkClient.executeRequest(request)
    }.fold(
        onSuccess = { response ->
            eventEmitter.emitIfPresent(response)
            when {
                /**
                 * HTTP_ACCEPTED (202) means the processing hasn't been completed, and API
                 * attaches an error body with it.
                 *
                 * Mapping 202s as API errors allow upper layers to customize handling.
                 */
                response.code == HttpURLConnection.HTTP_ACCEPTED -> throw handleApiError(response)
                response.isError -> throw handleApiError(response)
                else -> decodeResponse(requireNotNull(response.body))
            }
        },
        onFailure = {
            throw APIConnectionException(
                "Failed to execute $request",
                cause = it
            )
        }
    )

    @Throws(
        InvalidRequestException::class,
        AuthenticationException::class,
        APIException::class
    )
    private fun handleApiError(response: StripeResponse<String>): Exception {
        val requestId = response.requestId?.value
        val responseCode = response.code
        val stripeError = StripeErrorJsonParser().parse(response.responseJson())
        throw when (responseCode) {
            HttpURLConnection.HTTP_ACCEPTED,
            HttpURLConnection.HTTP_BAD_REQUEST,
            HttpURLConnection.HTTP_NOT_FOUND -> InvalidRequestException(
                stripeError,
                requestId,
                responseCode
            )
            HttpURLConnection.HTTP_UNAUTHORIZED -> AuthenticationException(stripeError, requestId)
            HttpURLConnection.HTTP_FORBIDDEN -> PermissionException(stripeError, requestId)
            HTTP_TOO_MANY_REQUESTS -> RateLimitException(stripeError, requestId)
            else -> APIException(stripeError, requestId, responseCode)
        }
    }
}
