package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A helper class for parsing errors coming from Stripe servers.
 */
class ErrorParser {

    @VisibleForTesting
    static final String MALFORMED_RESPONSE_MESSAGE =
            "An improperly formatted error response was found.";

    private static final String FIELD_CHARGE = "charge";
    private static final String FIELD_CODE = "code";
    private static final String FIELD_DECLINE_CODE = "decline_code";
    private static final String FIELD_ERROR = "error";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_PARAM = "param";
    private static final String FIELD_TYPE = "type";

    @NonNull
    static StripeError parseError(@Nullable String rawError) {
        String charge = null;
        String code = null;
        String declineCode = null;
        String message;
        String param = null;
        String type = null;
        try {
            JSONObject jsonError = new JSONObject(rawError);
            JSONObject errorObject = jsonError.getJSONObject(FIELD_ERROR);
            charge = errorObject.optString(FIELD_CHARGE);
            code = errorObject.optString(FIELD_CODE);
            declineCode = errorObject.optString(FIELD_DECLINE_CODE);
            message = errorObject.optString(FIELD_MESSAGE);
            param = errorObject.optString(FIELD_PARAM);
            type = errorObject.optString(FIELD_TYPE);
        } catch (JSONException jsonException) {
            message = MALFORMED_RESPONSE_MESSAGE;
        }
        return new StripeError(type, message, code, param, declineCode, charge);
    }

}
