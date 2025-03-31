package com.stripe.android.financialconnections.lite.network

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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection

internal class FinancialConnectionsLiteRequestExecutor(
    private val stripeNetworkClient: StripeNetworkClient,
    private val json: Json,
    private val logger: Logger,
) {
    suspend fun <Response> execute(
        request: StripeRequest,
        responseSerializer: KSerializer<Response>
    ): Result<Response> {
        return executeInternal(request) { body ->
            runCatching {
                json.decodeFromString(responseSerializer, body)
            }
        }
    }

    private suspend fun <Response> executeInternal(
        request: StripeRequest,
        decodeResponse: (String) -> Result<Response>,
    ): Result<Response> {
        logger.debug("Executing ${request.method.code} request to ${request.url}")
        return runCatching {
            stripeNetworkClient.executeRequest(request)
        }.mapCatching { response ->
            if (response.isError) {
                throw handleApiError(response)
            } else {
                decodeResponse(requireNotNull(response.body)).getOrThrow()
            }
        }.recoverCatching {
            throw APIConnectionException("Failed to execute $request", cause = it)
        }
    }

    private fun handleApiError(response: StripeResponse<String>): Exception {
        val requestId = response.requestId?.value
        val responseCode = response.code
        val stripeError = StripeErrorJsonParser().parse(response.responseJson())

        return when (responseCode) {
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
