package com.stripe.android.exception;

/**
 * An {@link Exception} that represents an internal problem with Stripe's servers.
 */
public class APIException extends StripeException {

    public APIException(String message, String requestId, Integer statusCode, Throwable e) {
        super(message, requestId, statusCode, e);
    }
}
