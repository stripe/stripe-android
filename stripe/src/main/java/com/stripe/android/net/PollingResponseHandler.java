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

}
