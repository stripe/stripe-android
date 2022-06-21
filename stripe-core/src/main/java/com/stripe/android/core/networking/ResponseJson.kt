package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.APIException
import org.json.JSONException
import org.json.JSONObject

@Throws(APIException::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
