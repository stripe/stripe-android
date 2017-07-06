package com.stripe.android;

import android.support.annotation.NonNull;
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
    static StripeError parseError(String rawError) {
        StripeError stripeError = new StripeError();
        try {
            JSONObject jsonError = new JSONObject(rawError);
            JSONObject errorObject = jsonError.getJSONObject(FIELD_ERROR);
            stripeError.charge = errorObject.optString(FIELD_CHARGE);
            stripeError.code = errorObject.optString(FIELD_CODE);
            stripeError.decline_code = errorObject.optString(FIELD_DECLINE_CODE);
            stripeError.message = errorObject.optString(FIELD_MESSAGE);
            stripeError.param = errorObject.optString(FIELD_PARAM);
            stripeError.type = errorObject.optString(FIELD_TYPE);
        } catch (JSONException jsonException) {
            stripeError.message = MALFORMED_RESPONSE_MESSAGE;
        }
        return stripeError;
    }

    /**
     * A model for error objects sent from the server.
     */
    static class StripeError {
        public String type;

        public String message;

        public String code;

        public String param;

        public String decline_code;

        public String charge;
    }
}
