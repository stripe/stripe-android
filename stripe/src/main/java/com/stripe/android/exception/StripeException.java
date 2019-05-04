package com.stripe.android.exception;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.StripeError;

/**
 * A base class for Stripe-related {@link Exception Exceptions}.
 */
public abstract class StripeException extends Exception {

    protected static final long serialVersionUID = 1L;

    @Nullable private final String mRequestId;
    @Nullable private final Integer mStatusCode;
    @Nullable private final StripeError mStripeError;

    public StripeException(@Nullable String message, @Nullable String requestId,
                           @Nullable Integer statusCode) {
        this(null, message, requestId, statusCode);
    }

    public StripeException(@Nullable StripeError stripeError, @Nullable String message,
                           @Nullable String requestId, @Nullable Integer statusCode) {
        this(stripeError, message, requestId, statusCode, null);
    }

    public StripeException(@Nullable String message, @Nullable String requestId,
                           @Nullable Integer statusCode, @Nullable Throwable e) {
        this(null, message, requestId, statusCode, e);
    }

    public StripeException(@Nullable StripeError stripeError, @Nullable String message,
                           @Nullable String requestId, @Nullable Integer statusCode,
                           @Nullable Throwable e) {
        super(message, e);
        mStripeError = stripeError;
        mStatusCode = statusCode;
        mRequestId = requestId;
    }

    @Nullable
    public String getRequestId() {
        return mRequestId;
    }

    @Nullable
    public Integer getStatusCode() {
        return mStatusCode;
    }

    @Nullable
    public StripeError getStripeError() {
        return mStripeError;
    }

    @NonNull
    @Override
    public String toString() {
        final String reqIdStr;
        if (mRequestId != null) {
            reqIdStr = "; request-id: " + mRequestId;
        } else {
            reqIdStr = "";
        }
        return super.toString() + reqIdStr;
    }
}

