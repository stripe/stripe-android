package com.stripe.android.exception;

import android.support.annotation.Nullable;

/**
 * An {@link Exception} that represents a failure to authenticate yourself to the server.
 */
public class AuthenticationException extends StripeException {

    public AuthenticationException(@Nullable String message, @Nullable String requestId,
                                   @Nullable Integer statusCode) {
        super(message, requestId, statusCode);
    }
}
