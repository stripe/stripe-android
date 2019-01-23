package com.stripe.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Represents a listener for Ephemeral Key Update events.
 */
public interface EphemeralKeyUpdateListener {

    /**
     * Called when a key update request from your server comes back successfully.
     *
     * @param rawJsonBodyReturnedByStripe the raw String returned from Stripe's servers
     */
    void onKeyUpdate(@NonNull String rawJsonBodyReturnedByStripe);

    /**
     * Called when a key update request from your server comes back with an error.
     *
     * @param responseCode the error code returned from Stripe's servers
     * @param message the error message returned from Stripe's servers
     */
    void onKeyUpdateFailure(int responseCode, @Nullable String message);

}
