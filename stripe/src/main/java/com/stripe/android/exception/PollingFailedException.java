package com.stripe.android.exception;


/**
 * Exception indicating that a polling request has expired with no change to a source.
 */
public class PollingFailedException extends StripeException {

    private boolean mIsExpired;

    public PollingFailedException(String message, boolean isExpired) {
        super(message, null, 0);
        mIsExpired = isExpired;
    }

    public boolean isExpired() {
        return mIsExpired;
    }
}
