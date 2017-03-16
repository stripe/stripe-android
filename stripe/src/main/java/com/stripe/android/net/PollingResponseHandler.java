package com.stripe.android.net;

/**
 * Represents a callback for the response of a polling operation.
 */
public interface PollingResponseHandler {

    /**
     * Called when polling is complete.
     *
     * @param pollingResponse the {@link PollingResponse} of this operation
     */
    void onPollingResponse(PollingResponse pollingResponse);

    /**
     * Called when the polling process is going to check again after a delay.
     *
     * @param millis the amount of delay until the next network check
     */
    void onRetry(int millis);

}
