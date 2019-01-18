package com.stripe.android.exception;

import android.support.annotation.Nullable;

/**
 * An {@link Exception} indicating that too many requests have hit the API too quickly.
 */
public class RateLimitException extends InvalidRequestException {

    public RateLimitException(
            @Nullable String message,
            @Nullable String param,
            @Nullable String requestId,
            @Nullable Integer statusCode,
            @Nullable Throwable e) {
        super(message, param, requestId, statusCode, null, null, e);
    }
}
