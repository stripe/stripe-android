package com.stripe.android.net;

import com.stripe.android.exception.StripeException;

/**
 * Represents a callback for the repsonse of a polling operation.
 */
public interface PollingResponseHandler {
    void onSuccess();
    void onRetry(int retryCount);
    void onError(StripeException stripeEx);
}
