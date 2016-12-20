package com.stripe.android.exception;

/**
 * An {@link Exception} that represents a failure to connect to Stripe's API.
 */
public class APIConnectionException extends StripeException {

    public APIConnectionException(String message) {
        super(message, null, 0);
    }

    public APIConnectionException(String message, Throwable e) {
        super(message, null, 0, e);
    }

}
