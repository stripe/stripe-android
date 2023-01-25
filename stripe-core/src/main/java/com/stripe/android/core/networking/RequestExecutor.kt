@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.StripeErrorJsonParser

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
                throw APIException(
                    stripeError = stripeErrorJsonParser.parse(response.responseJson()),
                    requestId = response.requestId?.value,
                    statusCode = response.code
                )
            } else {
                responseJsonParser.parse(response.responseJson()) ?: run {
                    throw APIException(
                        message = "$responseJsonParser returns null for ${response.responseJson()}"
                    )
                }
            }
        },
        onFailure = {
            throw APIConnectionException(
                "Failed to execute $request",
                cause = it
            )
        }
    )
}
