@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.ModelJsonParserAdapter
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.model.parsers.StripeModelParser
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.json.JSONException

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
                response.parseStripeModel(
                    parser = ModelJsonParserAdapter(responseJsonParser),
                    parserName = responseJsonParser::class.java.simpleName,
                    nullParseMessage = { responseBody ->
                        "$responseJsonParser returns null for $responseBody"
                    }
                )
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
                    response.parseStripeModel(
                        parser = ModelJsonParserAdapter(responseJsonParser),
                        parserName = responseJsonParser::class.java.simpleName,
                        nullParseMessage = { responseBody ->
                            "$responseJsonParser returns null for $responseBody"
                        }
                    )
                }.fold(
                    onSuccess = { Result.success(it) },
                    onFailure = { error ->
                        Result.failure(
                            error as? APIException ?: APIException(
                                message = "$responseJsonParser returns null for ${response.body ?: "{}"}",
                                cause = error
                            )
                        )
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

internal fun apiException(
    stripeErrorJsonParser: StripeErrorJsonParser,
    response: StripeResponse<String>
): APIException {
    val stripeError = try {
        stripeErrorJsonParser.parse(response.responseJsonObject())
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

internal fun connectionException(request: StripeRequest, cause: Throwable) = APIConnectionException(
    "Failed to execute $request",
    cause = cause
)

private fun <Response : StripeModel> StripeResponse<String>.parseStripeModel(
    parser: StripeModelParser<Response>,
    parserName: String = parser::class.java.simpleName,
    nullParseMessage: (responseBody: String) -> String = {
        "Unable to parse response with $parserName"
    }
): Response {
    val responseBody = body ?: "{}"

    val parsedResponse = try {
        parser.parse(responseBody)
    } catch (e: JSONException) {
        throw APIException(
            message =
            """
                Exception while parsing response body.
                  Status code: $code
                  Request-Id: $requestId
                  Content-Type: ${getHeaderValue(HEADER_CONTENT_TYPE)?.firstOrNull()}
                  Body: "$responseBody"
            """.trimIndent(),
            cause = e
        )
    }

    return parsedResponse ?: throw APIException(
        message = nullParseMessage(responseBody)
    )
}
