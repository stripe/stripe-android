package com.stripe.android.exception;

import androidx.annotation.Nullable;

import com.stripe.android.StripeError;

/**
 * An {@link Exception} indicating that too many requests have hit the API too quickly.
 */
public class RateLimitException extends InvalidRequestException {

    public RateLimitException(
            @Nullable String message,
            @Nullable String param,
            @Nullable String requestId,
            @Nullable Integer statusCode,
            @Nullable StripeError stripeError) {
        super(message, param, requestId, statusCode, null, null, stripeError, null);
    }
}
