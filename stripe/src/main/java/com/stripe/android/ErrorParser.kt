package com.stripe.android

import androidx.annotation.VisibleForTesting
import org.json.JSONException
import org.json.JSONObject

/**
 * A helper class for parsing errors coming from Stripe servers.
 */
internal object ErrorParser {

    @VisibleForTesting
    const val MALFORMED_RESPONSE_MESSAGE = "An improperly formatted error response was found."

    private const val FIELD_CHARGE = "charge"
    private const val FIELD_CODE = "code"
    private const val FIELD_DECLINE_CODE = "decline_code"
    private const val FIELD_ERROR = "error"
    private const val FIELD_MESSAGE = "message"
    private const val FIELD_PARAM = "param"
    private const val FIELD_TYPE = "type"

    @JvmSynthetic
    internal fun parseError(errorData: JSONObject): StripeError {
        return try {
            val errorObject = errorData.getJSONObject(FIELD_ERROR)
            StripeError(
                charge = errorObject.optString(FIELD_CHARGE),
                code = errorObject.optString(FIELD_CODE),
                declineCode = errorObject.optString(FIELD_DECLINE_CODE),
                message = errorObject.optString(FIELD_MESSAGE),
                param = errorObject.optString(FIELD_PARAM),
                type = errorObject.optString(FIELD_TYPE)
            )
        } catch (jsonException: JSONException) {
            MALFORMED
        }
    }

    @JvmSynthetic
    internal val MALFORMED = StripeError(message = MALFORMED_RESPONSE_MESSAGE)
}
