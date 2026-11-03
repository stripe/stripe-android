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
import org.json.JSONObject

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
                runCatching {
                    response.responseJson()
                }.fold(
                    onSuccess = { json ->
                        responseJsonParser.parseForResult(json)
                    },
                    onFailure = { jsonError ->
                        Result.failure(jsonError)
                    }
                )
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

private fun <Response : StripeModel> ModelJsonParser<Response>.parseForResult(
    json: JSONObject
): Result<Response> {
    return runCatching {
        parse(json)
    }.fold(
        onSuccess = { parsedResponse ->
            if (parsedResponse != null) {
                Result.success(parsedResponse)
            } else {
                Result.failure(
                    APIException(
                        message = "$this returns null for $json"
                    )
                )
            }
        },
        onFailure = { parseError ->
            Result.failure(parseError)
        }
    )
}

private fun apiException(
    stripeErrorJsonParser: StripeErrorJsonParser,
    response: StripeResponse<String>
): APIException {
    val stripeError = try {
        stripeErrorJsonParser.parse(response.responseJson())
    } catch (_: APIException) {
        null
    }
    return APIException(
        stripeError = stripeError,
        requestId = response.requestId?.value,
        statusCode = response.code,
        message = stripeError?.message ?: "Request failed with status code ${response.code} and non-JSON error body."
    )
}

private fun connectionException(request: StripeRequest, cause: Throwable) = APIConnectionException(
    "Failed to execute $request",
    cause = cause
)
