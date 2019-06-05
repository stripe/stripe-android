package com.stripe.android.exception;

import android.support.annotation.Nullable;

import com.stripe.android.StripeError;

/**
 * An {@link Exception} indicating that too many requests have hit the API too quickly.
 */
public final class RateLimitException extends InvalidRequestException {

    public RateLimitException(
            @Nullable String message,
            @Nullable String param,
            @Nullable String requestId,
            @Nullable StripeError stripeError) {
        super(message, param, requestId, 429, null, null, stripeError, null);
    }
}
