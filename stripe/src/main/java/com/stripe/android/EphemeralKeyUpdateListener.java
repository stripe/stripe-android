package com.stripe.android;

import android.support.annotation.NonNull;

/**
 * Represents a listener for Ephemeral Key Update events.
 */
public interface EphemeralKeyUpdateListener {

    /**
     * Called when a key update request from your server comes back successfully.
     *
     * @param stripeResponseJson the raw JSON String returned from Stripe's servers
     */
    void onKeyUpdate(@NonNull String stripeResponseJson);

    /**
     * Called when a key update request from your server comes back with an error.
     *
     * @param responseCode the error code returned from Stripe's servers
     * @param message the error message returned from Stripe's servers
     */
    void onKeyUpdateFailure(int responseCode, @NonNull String message);

}
