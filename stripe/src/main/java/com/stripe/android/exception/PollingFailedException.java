package com.stripe.android.exception;


/**
 * Exception indicating that a polling request has failed to discover a change in the source.
 */
public class PollingFailedException extends StripeException {

    private boolean mIsExpired;

    /**
     * Create a polling failure exception with a description of the failure and whether or not it
     * expired.
     *
     * @param message a message explaining the cause of the failure
     * @param isExpired whether or not the polling request expired
     */
    public PollingFailedException(String message, boolean isExpired) {
        super(message, null, 0);
        mIsExpired = isExpired;
    }

    /**
     * @return Whether or not this failed because of a timeout in the polling operation.
     */
    public boolean isExpired() {
        return mIsExpired;
    }
}
