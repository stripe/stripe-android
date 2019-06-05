package com.stripe.android;

import android.support.annotation.Nullable;

import java.io.Serializable;

/**
 * A model for error objects sent from the Stripe API.
 *
 * https://stripe.com/docs/api/errors
 */
public final class StripeError implements Serializable {

    // https://stripe.com/docs/api/errors (e.g. "invalid_request_error")
    @Nullable public final String type;
    @Nullable public final String message;

    // https://stripe.com/docs/error-codes (e.g. "payment_method_unactivated")
    @Nullable public final String code;
    @Nullable public final String param;

    // see https://stripe.com/docs/declines/codes
    @Nullable public final String declineCode;

    @Nullable public final String charge;

    StripeError(@Nullable String type, @Nullable String message, @Nullable String code,
                @Nullable String param, @Nullable String declineCode, @Nullable String charge) {
        this.type = type;
        this.message = message;
        this.code = code;
        this.param = param;
        this.declineCode = declineCode;
        this.charge = charge;
    }
}
