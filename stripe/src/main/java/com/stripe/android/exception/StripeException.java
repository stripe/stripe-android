package com.stripe.android.exception;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.StripeError;

/**
 * A base class for Stripe-related {@link Exception Exceptions}.
 */
public abstract class StripeException extends Exception {

    protected static final long serialVersionUID = 1L;

    @Nullable private final String mRequestId;
    private final int mStatusCode;
    @Nullable private final StripeError mStripeError;

    public StripeException(@Nullable StripeError stripeError, @Nullable String message,
                           @Nullable String requestId, int statusCode) {
        this(stripeError, message, requestId, statusCode, null);
    }

    public StripeException(@Nullable String message, @Nullable String requestId,
                           int statusCode, @Nullable Throwable e) {
        this(null, message, requestId, statusCode, e);
    }

    public StripeException(@Nullable StripeError stripeError, @Nullable String message,
                           @Nullable String requestId, int statusCode, @Nullable Throwable e) {
        super(message, e);
        mStripeError = stripeError;
        mStatusCode = statusCode;
        mRequestId = requestId;
    }

    @Nullable
    public String getRequestId() {
        return mRequestId;
    }

    public int getStatusCode() {
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

