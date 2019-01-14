package com.stripe.android.exception;


import android.support.annotation.Nullable;

/**
 * An {@link Exception} indicating that invalid parameters were used in a request.
 */
public class InvalidRequestException extends StripeException {

    private final String param;
    private final String code;

    public InvalidRequestException(String message,
                                   String param,
                                   String code,
                                   String requestId,
                                   Integer statusCode,
                                   Throwable e) {
        super(message, requestId, statusCode, e);
        this.param = param;
        this.code = code;
    }

    public InvalidRequestException(String message, String param, String requestId, Integer
            statusCode, Throwable e) {
        super(message, requestId, statusCode, e);
        this.param = param;
        this.code = null;
    }

    public String getParam() {
        return param;
    }

    @Nullable
    public String getCode() {
        return code;
    }

}
