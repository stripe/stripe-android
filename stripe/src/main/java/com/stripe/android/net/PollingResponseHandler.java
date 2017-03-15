package com.stripe.android.net;

import com.stripe.android.exception.PollingFailedException;
import com.stripe.android.exception.StripeException;

/**
 * Represents a callback for the response of a polling operation.
 */
public interface PollingResponseHandler {

    /**
     * Called when the update has come back as a success.
     */
    void onSuccess();

    /**
     * Called when the polling process is going to check again after a delay.
     *
     * @param millis the amount of delay until the next network check
     */
    void onRetry(int millis);

    /**
     * Called when the polling process has returned some kind of failure.
     *
     * @param stripeEx an {@link StripeException} resulting from the polling process, which may
     *                 include a {@link PollingFailedException} for timeout or failure response
     */
    void onError(StripeException stripeEx);
}
