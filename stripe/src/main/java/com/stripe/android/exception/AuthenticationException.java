package com.stripe.android.exception;

import androidx.annotation.Nullable;

import com.stripe.android.StripeError;

/**
 * An {@link Exception} that represents a failure to authenticate yourself to the server.
 */
public class AuthenticationException extends StripeException {

    public AuthenticationException(@Nullable String message, @Nullable String requestId,
                                   @Nullable Integer statusCode,
                                   @Nullable StripeError stripeError) {
        super(stripeError, message, requestId, statusCode);
    }
}
