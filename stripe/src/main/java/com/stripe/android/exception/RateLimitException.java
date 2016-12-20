package com.stripe.android.exception;

/**
 * An {@link Exception} indicating that too many requests have hit the API too quickly.
 */
public class RateLimitException extends InvalidRequestException {

    public RateLimitException(
            String message,
            String param,
            String requestId,
            Integer statusCode,
            Throwable e) {
        super(message, param, requestId, statusCode, e);
    }
}
