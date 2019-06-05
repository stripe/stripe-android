package com.stripe.android.exception;

import android.support.annotation.Nullable;

/**
 * An {@link Exception} that represents a failure to connect to Stripe's API.
 */
public class APIConnectionException extends StripeException {
    private static final int STATUS_CODE = 0;

    public APIConnectionException(@Nullable String message, @Nullable Throwable e) {
        super(null, message, null, STATUS_CODE, e);
    }

}
