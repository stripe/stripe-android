package com.stripe.android.net;

import android.support.annotation.Nullable;

import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Source;

/**
 * A data model for a polling response.
 *
 * @deprecated Polling Stripe sources is deprecated, and not guaranteed to be supported beyond
 * 4.X.X library updates.
 */
@Deprecated
public class PollingResponse {

    private boolean mIsExpired;
    private boolean mIsSuccess;
    private Source mSource;
    private StripeException mStripeException;

    PollingResponse(@Nullable Source source, boolean isSuccess, boolean isExpired) {
        mSource = source;
        mIsExpired = isExpired;
        mIsSuccess = isSuccess;
    }

    PollingResponse(@Nullable Source source, StripeException stripeException) {
        mSource = source;
        mStripeException = stripeException;
        mIsExpired = false;
        mIsSuccess = false;
    }

    @Nullable
    public Source getSource() {
        return mSource;
    }

    @Nullable
    public StripeException getStripeException() {
        return mStripeException;
    }

    public boolean isExpired() {
        return mIsExpired;
    }

    public boolean isSuccess() {
        return mIsSuccess;
    }
}
