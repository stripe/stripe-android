package com.stripe.android.core.model.parsers

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.StripeError
import com.stripe.android.core.model.StripeJsonUtils.optString
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StripeErrorJsonParser : ModelJsonParser<StripeError> {

    override fun parse(json: JSONObject): StripeError {
        return runCatching {
            json.getJSONObject(FIELD_ERROR).let { errorObject ->
                StripeError(
                    charge = optString(errorObject, FIELD_CHARGE),
                    code = optString(errorObject, FIELD_CODE),
                    declineCode = optString(errorObject, FIELD_DECLINE_CODE),
                    message = optString(errorObject, FIELD_MESSAGE),
                    param = optString(errorObject, FIELD_PARAM),
                    type = optString(errorObject, FIELD_TYPE),
                    docUrl = optString(errorObject, FIELD_DOC_URL),
                    extraFields = errorObject
                        .optJSONObject(FIELD_EXTRA_FIELDS)?.let { extraFieldsJson ->
                            extraFieldsJson.keys().asSequence()
                                .map { key -> key to extraFieldsJson.get(key).toString() }
                                .toMap()
                        }
                )
            }
        }.getOrDefault(
            StripeError(
                message = MALFORMED_RESPONSE_MESSAGE
            )
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
