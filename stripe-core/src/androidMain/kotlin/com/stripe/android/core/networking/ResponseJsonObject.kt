package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.APIException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

@Throws(APIException::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun StripeResponse<String>.responseJsonObject(
    json: Json = Json
): JsonObject {
    val body = body ?: return buildJsonObject {}

    return try {
        json.parseToJsonElement(body).jsonObject
    } catch (e: Exception) {
        throw APIException(
            message =
            """
                Exception while parsing response body.
                  Status code: $code
                  Request-Id: $requestId
                  Content-Type: ${getHeaderValue(HEADER_CONTENT_TYPE)?.firstOrNull()}
                  Body: "$body"
            """.trimIndent(),
            cause = e
        )
    }
}
