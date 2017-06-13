package com.stripe.android.net;

/**
 * Represents a callback for the response of a polling operation.
 *
 * @deprecated Polling Stripe sources is deprecated, and not guaranteed to be supported beyond
 * 4.X.X library updates.
 */
@Deprecated
public interface PollingResponseHandler {

    /**
     * Called when polling is complete.
     *
     * @param pollingResponse the {@link PollingResponse} of this operation
     */
    void onPollingResponse(PollingResponse pollingResponse);

}
