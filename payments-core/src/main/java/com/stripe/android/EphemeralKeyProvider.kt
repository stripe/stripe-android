package com.stripe.android

import androidx.annotation.Size

/**
 * Represents an object that can call to a server and create
 * [EphemeralKeys][EphemeralKey].
 */
fun interface EphemeralKeyProvider {

    /**
     * When called, talks to a client server that then communicates with Stripe's servers to
     * create an [EphemeralKey].
     *
     * @param apiVersion the Stripe API Version being used
     * @param keyUpdateListener a callback object to notify about results
     */
    fun createEphemeralKey(
        @Size(min = 4) apiVersion: String,
        keyUpdateListener: EphemeralKeyUpdateListener
    )
}
