package com.stripe.android.exception;

import androidx.annotation.Nullable;

/**
 * A type of {@link AuthenticationException} resulting from incorrect permissions
 * to perform the requested action.
 */
public class PermissionException extends AuthenticationException {

    public PermissionException(@Nullable String message, @Nullable String requestId,
                               @Nullable Integer statusCode) {
        super(message, requestId, statusCode);
    }
}
