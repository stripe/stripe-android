package com.stripe.android.exception;

import androidx.annotation.Nullable;

import com.stripe.android.StripeError;

import java.net.HttpURLConnection;

/**
 * A type of {@link AuthenticationException} resulting from incorrect permissions
 * to perform the requested action.
 */
public class PermissionException extends AuthenticationException {

    public PermissionException(@Nullable String message, @Nullable String requestId,
                               @Nullable StripeError stripeError) {
        super(message, requestId, HttpURLConnection.HTTP_FORBIDDEN, stripeError);
    }
}
