package com.stripe.android.exception;


import android.support.annotation.Nullable;

import com.stripe.android.StripeError;

/**
 * An {@link Exception} indicating that invalid parameters were used in a request.
 */
public class InvalidRequestException extends StripeException {

    @Nullable private final String mParam;
    @Nullable private final String mErrorCode;
    @Nullable private final String mErrorDeclineCode;

    public InvalidRequestException(@Nullable String message, @Nullable String param,
                                   @Nullable String requestId, @Nullable Integer statusCode,
                                   @Nullable String errorCode, @Nullable String errorDeclineCode,
                                   @Nullable StripeError stripeError, @Nullable Throwable e) {
        super(stripeError, message, requestId, statusCode, e);
        mParam = param;
        mErrorCode = errorCode;
        mErrorDeclineCode = errorDeclineCode;
    }

    @Nullable
    public String getParam() {
        return mParam;
    }

    @Nullable
    public String getErrorCode() {
        return mErrorCode;
    }

    @Nullable
    public String getErrorDeclineCode() {
        return mErrorDeclineCode;
    }
}
