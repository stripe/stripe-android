package com.stripe.android.core.model.parsers

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.StripeError
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StripeErrorJsonParser {

    fun parse(json: JsonObject): StripeError {
        return runCatching {
            val errorObject = json[FIELD_ERROR] as? JsonObject
                ?: return malformedStripeError()

            StripeError(
                charge = errorObject.optString(FIELD_CHARGE),
                code = errorObject.optString(FIELD_CODE),
                declineCode = errorObject.optString(FIELD_DECLINE_CODE),
                message = errorObject.optString(FIELD_MESSAGE),
                param = errorObject.optString(FIELD_PARAM),
                type = errorObject.optString(FIELD_TYPE),
                docUrl = errorObject.optString(FIELD_DOC_URL),
                extraFields = (errorObject[FIELD_EXTRA_FIELDS] as? JsonObject)?.mapValues { (_, value) ->
                    value.toLegacyString()
                }
            )
        }.getOrDefault(
            malformedStripeError()
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        @VisibleForTesting
        internal const val MALFORMED_RESPONSE_MESSAGE =
            "An improperly formatted error response was found."

        internal const val FIELD_CHARGE = "charge"
        internal const val FIELD_CODE = "code"
        internal const val FIELD_DECLINE_CODE = "decline_code"
        internal const val FIELD_EXTRA_FIELDS = "extra_fields"
        internal const val FIELD_DOC_URL = "doc_url"
        internal const val FIELD_ERROR = "error"
        internal const val FIELD_MESSAGE = "message"
        internal const val FIELD_PARAM = "param"
        internal const val FIELD_TYPE = "type"
    }
}

private fun JsonObject.optString(fieldName: String): String? {
    return nullIfNullOrEmpty(this[fieldName].toStripeErrorStringOrNull())
}

private fun JsonElement?.toStripeErrorStringOrNull(): String? {
    return when (this) {
        null,
        JsonNull -> null
        is JsonPrimitive -> content
        else -> toString()
    }
}

private fun JsonElement.toLegacyString(): String {
    return when (this) {
        JsonNull -> "null"
        is JsonPrimitive -> content
        else -> toString()
    }
}

private fun nullIfNullOrEmpty(value: String?): String? {
    return value?.takeUnless { it == "null" || it.isEmpty() }
}

private fun malformedStripeError(): StripeError {
    return StripeError(
        message = StripeErrorJsonParser.MALFORMED_RESPONSE_MESSAGE
    )
}
