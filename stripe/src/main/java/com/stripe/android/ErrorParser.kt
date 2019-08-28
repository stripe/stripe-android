package com.stripe.android

import android.support.annotation.VisibleForTesting
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

    @JvmStatic
    fun parseError(rawError: String?): StripeError {
        var charge: String? = null
        var code: String? = null
        var declineCode: String? = null
        var message: String
        var param: String? = null
        var type: String? = null
        try {
            val jsonError = JSONObject(rawError)
            val errorObject = jsonError.getJSONObject(FIELD_ERROR)
            charge = errorObject.optString(FIELD_CHARGE)
            code = errorObject.optString(FIELD_CODE)
            declineCode = errorObject.optString(FIELD_DECLINE_CODE)
            message = errorObject.optString(FIELD_MESSAGE)
            param = errorObject.optString(FIELD_PARAM)
            type = errorObject.optString(FIELD_TYPE)
        } catch (jsonException: JSONException) {
            message = MALFORMED_RESPONSE_MESSAGE
        }

        return StripeError(type, message, code, param, declineCode, charge)
    }
}
