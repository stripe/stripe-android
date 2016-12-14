package com.stripe.android.net;

import android.support.annotation.NonNull;

import com.stripe.android.util.StripeJsonUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A helper class for parsing errors coming from Stripe servers.
 */
class ErrorParser {

    private static final String FIELD_CHARGE = "charge";
    private static final String FIELD_CODE = "code";
    private static final String FIELD_DECLINE_CODE = "decline_code";
    private static final String FIELD_ERROR = "error";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_PARAM = "param";
    private static final String FIELD_TYPE = "type";

    private static final String MALFORMED_RESPONSE_MESSAGE =
            "An improperly formatted error response was found.";

    @NonNull
    static Error parseError(String rawError) {
        Error error = new Error();
        try {
            JSONObject jsonError = new JSONObject(rawError);
            JSONObject errorObject = jsonError.getJSONObject(FIELD_ERROR);
            error.charge = StripeJsonUtils.optString(errorObject, FIELD_CHARGE);
            error.code = StripeJsonUtils.optString(errorObject, FIELD_CODE);
            error.decline_code = StripeJsonUtils.optString(errorObject, FIELD_DECLINE_CODE);
            error.message = StripeJsonUtils.optString(errorObject, FIELD_MESSAGE);
            error.param = StripeJsonUtils.optString(errorObject, FIELD_PARAM);
            error.type = StripeJsonUtils.optString(errorObject, FIELD_TYPE);
        } catch (JSONException jsonException) {
            error.message = MALFORMED_RESPONSE_MESSAGE;
        }
        return error;
    }

    /**
     * A model for error objects sent from the server.
     */
    static class Error {
        public String type;

        public String message;

        public String code;

        public String param;

        public String decline_code;

        public String charge;
    }
}
