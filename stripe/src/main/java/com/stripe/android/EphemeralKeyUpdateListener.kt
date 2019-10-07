package com.stripe.android

/**
 * Represents a listener for Ephemeral Key Update events.
 */
interface EphemeralKeyUpdateListener {

    /**
     * Called when a key update request from your server comes back successfully.
     *
     * @param stripeResponseJson the raw JSON String returned from Stripe's servers
     */
    fun onKeyUpdate(stripeResponseJson: String)

    /**
     * Called when a key update request from your server comes back with an error.
     *
     * @param responseCode the error code returned from Stripe's servers
     * @param message the error message returned from Stripe's servers
     */
    fun onKeyUpdateFailure(responseCode: Int, message: String)
}
