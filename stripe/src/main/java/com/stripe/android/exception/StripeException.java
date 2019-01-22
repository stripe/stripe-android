package com.stripe.android.exception;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A base class for Stripe-related {@link Exception Exceptions}.
 */
public abstract class StripeException extends Exception {

    protected static final long serialVersionUID = 1L;

    @Nullable private final String requestId;
    @Nullable private final Integer statusCode;

    public StripeException(@Nullable String message, @Nullable String requestId,
                           @Nullable Integer statusCode) {
        this(message, requestId, statusCode, null);
    }

    public StripeException(@Nullable String message, @Nullable String requestId,
                           @Nullable Integer statusCode, @Nullable Throwable e) {
        super(message, e);
        this.statusCode = statusCode;
        this.requestId = requestId;
    }

    @Nullable
    public String getRequestId() {
        return requestId;
    }

    @Nullable
    public Integer getStatusCode() {
        return statusCode;
    }

    @NonNull
    @Override
    public String toString() {
        final String reqIdStr;
        if (requestId != null) {
            reqIdStr = "; request-id: " + requestId;
        } else {
            reqIdStr = "";
        }
        return super.toString() + reqIdStr;
    }
}

