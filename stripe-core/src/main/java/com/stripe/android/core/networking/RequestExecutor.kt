@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
suspend fun <Response : StripeModel> executeRequestWithModelJsonParser(
    stripeNetworkClient: StripeNetworkClient,
    stripeErrorJsonParser: StripeErrorJsonParser,
    request: StripeRequest,
    responseJsonParser: ModelJsonParser<Response>,
): Response {
    return runCatching {
        stripeNetworkClient.executeRequest(request)
    }.fold(
        onSuccess = { response ->
            if (response.isError) {
                throw apiException(stripeErrorJsonParser, response)
            } else {
                responseJsonParser.parse(response.responseJson()) ?: run {
                    throw APIException(message = "$responseJsonParser returns null for ${response.responseJson()}")
                }
            }
        },
        onFailure = {
            throw connectionException(request, it)
        }
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
suspend fun <Response : StripeModel> executeRequestWithResultParser(
    stripeNetworkClient: StripeNetworkClient,
    stripeErrorJsonParser: StripeErrorJsonParser,
    request: StripeRequest,
    responseJsonParser: ModelJsonParser<Response>,
): Result<Response> {
    return runCatching {
        stripeNetworkClient.executeRequest(request)
    }.fold(
        onSuccess = { response ->
            if (response.isError) {
                Result.failure(apiException(stripeErrorJsonParser, response))
            } else {
                val parsedResponse = runCatching {
                    responseJsonParser.parse(response.responseJson())
                }.getOrNull()

                if (parsedResponse != null) {
                    Result.success(parsedResponse)
                } else {
                    Result.failure(
                        APIException(message = "$responseJsonParser returns null for ${response.responseJson()}")
                    )
                }
            }
        },
        onFailure = {
            Result.failure(connectionException(request, it))
        }
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
suspend fun executeRequestWithErrorParsing(
    stripeNetworkClient: StripeNetworkClient,
    stripeErrorJsonParser: StripeErrorJsonParser,
    request: StripeRequest,
): Result<Unit> {
    return runCatching {
        stripeNetworkClient.executeRequest(request)
    }.fold(
        onSuccess = { response ->
            if (response.isError) {
                Result.failure(apiException(stripeErrorJsonParser, response))
            } else {
                Result.success(Unit)
            }
        },
        onFailure = {
            Result.failure(connectionException(request, it))
        }
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
suspend fun <Response> executeRequestWithKSerializerParser(
    stripeNetworkClient: StripeNetworkClient,
    stripeErrorJsonParser: StripeErrorJsonParser,
    request: StripeRequest,
    responseSerializer: KSerializer<Response>,
    json: Json = Json
): Result<Response> {
    return runCatching {
        stripeNetworkClient.executeRequest(request)
    }.fold(
        onSuccess = { response ->
            if (response.isError) {
                Result.failure(apiException(stripeErrorJsonParser, response))
            } else {
                val parsedResponse = runCatching {
                    response.body?.let { body ->
                        json.decodeFromString(responseSerializer, body)
                    }
                }.getOrNull()

                if (parsedResponse != null) {
                    Result.success(parsedResponse)
                } else {
                    Result.failure(
                        APIException(message = "Failed to parse response JSON for ${response.body}")
                    )
                }
            }
        },
        onFailure = {
            Result.failure(connectionException(request, it))
        }
    )
}

private fun apiException(
    stripeErrorJsonParser: StripeErrorJsonParser,
    response: StripeResponse<String>
): APIException {
    return APIException(
        stripeError = stripeErrorJsonParser.parse(response.responseJson()),
        requestId = response.requestId?.value,
        statusCode = response.code
    )
}

private fun connectionException(request: StripeRequest, cause: Throwable) = APIConnectionException(
    "Failed to execute $request",
    cause = cause
)
