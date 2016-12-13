package com.stripe.android.exception;

/**
 * An {@link Exception} that represents a failure to authenticate yourself to the server.
 */
public class AuthenticationException extends StripeException {

    public AuthenticationException(String message, String requestId, Integer statusCode) {
        super(message, requestId, statusCode);
    }
}
