package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Represents a listener for Ephemeral Key Update events.
 */
public interface EphemeralKeyUpdateListener {

    /**
     * Called when a key update request from your server comes back successfully.
     *
     * @param rawKey the raw String returned from Stripe's servers
     */
    void onKeyUpdate(@NonNull String rawKey);

    /**
     * Called when a key update request from your server comes back with an error.
     *
     * @param responseCode the error code returned from Stripe's servers
     * @param message the error message returned from Stripe's servers
     */
    void onKeyUpdateFailure(int responseCode, @Nullable String message);

}
