package com.stripe.android.exception;

import androidx.annotation.Nullable;

import com.stripe.android.StripeError;

/**
 * An {@link Exception} that represents an internal problem with Stripe's servers.
 */
public class APIException extends StripeException {

    public APIException(@Nullable String message, @Nullable String requestId,
                        @Nullable Integer statusCode, @Nullable StripeError stripeError,
                        @Nullable Throwable e) {
        super(stripeError, message, requestId, statusCode, e);
    }
}
