package com.stripe.android.exception;

import androidx.annotation.Nullable;

/**
 * An {@link Exception} that represents a failure to connect to Stripe's API.
 */
public class APIConnectionException extends StripeException {

    public APIConnectionException(@Nullable String message) {
        this(message, null);
    }

    public APIConnectionException(@Nullable String message, @Nullable Throwable e) {
        super(message, null, 0, e);
    }

}
