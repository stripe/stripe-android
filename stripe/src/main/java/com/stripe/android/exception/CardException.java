package com.stripe.android.exception;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.StripeError;

/**
 * An {@link Exception} indicating that there is a problem with a Card used for a request.
 * Card errors are the most common type of error you should expect to handle.
 * They result when the user enters a card that can't be charged for some reason.
 */
public class CardException extends StripeException {

    @Nullable private final String mCode;
    @Nullable private final String mParam;
    @Nullable private final String mDeclineCode;
    @Nullable private final String mCharge;

    public CardException(@Nullable String message, @Nullable String requestId,
                         @Nullable String code, @Nullable String param,
                         @Nullable String declineCode, @Nullable String charge,
                         @Nullable Integer statusCode, @NonNull StripeError stripeError) {
        super(stripeError, message, requestId, statusCode);
        mCode = code;
        mParam = param;
        mDeclineCode = declineCode;
        mCharge = charge;
    }

    @Nullable
    public String getCode() {
        return mCode;
    }

    @Nullable
    public String getParam() {
        return mParam;
    }

    @Nullable
    public String getDeclineCode() {
        return mDeclineCode;
    }

    @Nullable
    public String getCharge() {
        return mCharge;
    }
}
