package com.stripe.android.exception;

import androidx.annotation.Nullable;

import com.stripe.android.StripeError;

import java.net.HttpURLConnection;

/**
 * An {@link Exception} that represents a failure to authenticate yourself to the server.
 */
public class AuthenticationException extends StripeException {

    public AuthenticationException(@Nullable String message, @Nullable String requestId,
                                   @Nullable StripeError stripeError) {
        this(message, requestId, HttpURLConnection.HTTP_UNAUTHORIZED, stripeError);
    }

    AuthenticationException(@Nullable String message, @Nullable String requestId,
                            int statusCode, @Nullable StripeError stripeError) {
        super(stripeError, message, requestId, statusCode);
    }
}
