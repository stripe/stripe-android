package com.stripe.android.exception;

/**
 * A type of {@link AuthenticationException} resulting from incorrect permissions
 * to perform the requested action.
 */
public class PermissionException extends AuthenticationException {

    public PermissionException(String message, String requestId, Integer statusCode) {
        super(message, requestId, statusCode);
    }
}
