package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Size;

/**
 * Represents an object that can call to a server and create
 * {@link AbstractEphemeralKey EphemeralKeys}.
 */
public interface EphemeralKeyProvider {

    /**
     * When called, talks to a client server that then communicates with Stripe's servers to
     * create an {@link AbstractEphemeralKey}.
     *
     * @param apiVersion the Stripe API Version being used
     * @param keyUpdateListener a callback object to notify about results
     */
    void createEphemeralKey(
            @NonNull @Size(min = 4) String apiVersion,
            @NonNull final EphemeralKeyUpdateListener keyUpdateListener);
}
