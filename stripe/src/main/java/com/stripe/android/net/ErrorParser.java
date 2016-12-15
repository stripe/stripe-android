package com.stripe.android.net;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.util.StripeJsonUtils;

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
            stripeError.charge = StripeJsonUtils.optString(errorObject, FIELD_CHARGE);
            stripeError.code = StripeJsonUtils.optString(errorObject, FIELD_CODE);
            stripeError.decline_code = StripeJsonUtils.optString(errorObject, FIELD_DECLINE_CODE);
            stripeError.message = StripeJsonUtils.optString(errorObject, FIELD_MESSAGE);
            stripeError.param = StripeJsonUtils.optString(errorObject, FIELD_PARAM);
            stripeError.type = StripeJsonUtils.optString(errorObject, FIELD_TYPE);
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
