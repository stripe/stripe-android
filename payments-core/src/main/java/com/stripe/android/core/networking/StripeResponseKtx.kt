package com.stripe.android.core.networking

import com.stripe.android.exception.APIException
import com.stripe.android.networking.RequestHeadersFactory.Companion.HEADER_CONTENT_TYPE
import org.json.JSONException
import org.json.JSONObject

@Throws(APIException::class)
fun StripeResponse<String>.responseJson(): JSONObject =
    body?.let {
        try {
            JSONObject(it)
        } catch (e: JSONException) {
            throw APIException(
                message =
                """
                    Exception while parsing response body.
                      Status code: $code
                      Request-Id: $requestId
                      Content-Type: ${getHeaderValue(HEADER_CONTENT_TYPE)?.firstOrNull()}
                      Body: "$it"
                """.trimIndent(),
                cause = e
            )
        }
    } ?: JSONObject()
