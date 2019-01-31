package com.stripe.android.exception;

import androidx.annotation.Nullable;

import com.stripe.android.StripeError;

/**
 * A type of {@link AuthenticationException} resulting from incorrect permissions
 * to perform the requested action.
 */
public class PermissionException extends AuthenticationException {

    public PermissionException(@Nullable String message, @Nullable String requestId,
                               @Nullable Integer statusCode, @Nullable StripeError stripeError) {
        super(message, requestId, statusCode, stripeError);
    }
}
