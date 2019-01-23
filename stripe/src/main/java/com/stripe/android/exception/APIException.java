package com.stripe.android.exception;

import androidx.annotation.Nullable;

/**
 * An {@link Exception} that represents an internal problem with Stripe's servers.
 */
public class APIException extends StripeException {

    public APIException(@Nullable String message, @Nullable String requestId,
                        @Nullable Integer statusCode, @Nullable Throwable e) {
        super(message, requestId, statusCode, e);
    }
}
