package com.stripe.android.crypto.onramp.model

/**
 * Provides the client secret for a given onramp session by invoking the merchant's backend.
 *
 * During checkout, the SDK calls [getClientSecret] to trigger a server-side call to
 * Stripe's `/v1/crypto/onramp_sessions/:id/checkout` endpoint. The returned client secret
 * is then used to retrieve the resulting PaymentIntent.
 *
 * This may be called more than once for a single checkout — once initially, and again
 * after any required payment authentication (e.g. 3DS) is handled.
 */
internal fun interface OnrampSessionClientSecretProvider {
    suspend fun getClientSecret(onrampSessionId: String): String
}
