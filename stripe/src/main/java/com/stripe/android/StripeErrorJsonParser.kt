package com.stripe.android

import androidx.annotation.VisibleForTesting
import com.stripe.android.model.StripeJsonUtils.optString
import com.stripe.android.model.parsers.ModelJsonParser
import org.json.JSONException
import org.json.JSONObject

internal class StripeErrorJsonParser : ModelJsonParser<StripeError> {

    override fun parse(json: JSONObject): StripeError {
        return try {
            json.getJSONObject(FIELD_ERROR).let { errorObject ->
                StripeError(
                    charge = optString(errorObject, FIELD_CHARGE),
                    code = optString(errorObject, FIELD_CODE),
                    declineCode = optString(errorObject, FIELD_DECLINE_CODE),
                    message = optString(errorObject, FIELD_MESSAGE),
                    param = optString(errorObject, FIELD_PARAM),
                    type = optString(errorObject, FIELD_TYPE),
                    docUrl = optString(errorObject, FIELD_DOC_URL)
                )
            }
        } catch (jsonException: JSONException) {
            StripeError(
                message = MALFORMED_RESPONSE_MESSAGE
            )
        }
    }

    internal companion object {
        @VisibleForTesting
        internal const val MALFORMED_RESPONSE_MESSAGE = "An improperly formatted error response was found."

        private const val FIELD_CHARGE = "charge"
        private const val FIELD_CODE = "code"
        private const val FIELD_DECLINE_CODE = "decline_code"
        private const val FIELD_DOC_URL = "doc_url"
        private const val FIELD_ERROR = "error"
        private const val FIELD_MESSAGE = "message"
        private const val FIELD_PARAM = "param"
        private const val FIELD_TYPE = "type"
    }
}
