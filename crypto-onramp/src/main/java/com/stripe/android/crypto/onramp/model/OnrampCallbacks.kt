package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

/**
 * Container for all callbacks required by the Onramp coordinator.
 *
 * This class groups together the set of callbacks that drive each step of the
 * onramp flow, from user authentication through checkout completion.
 *
 * Each callback represents a distinct stage in the onramp process and is
 * invoked by the coordinator at the appropriate time.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OnrampCallbacks {

    private var verifyIdentityCallback: OnrampVerifyIdentityCallback? = null
    private var verifyKycCallback: OnrampVerifyKycCallback? = null
    private var collectPaymentCallback: OnrampCollectPaymentMethodCallback? = null
    private var authorizeCallback: OnrampAuthorizeCallback? = null
    private var checkoutCallback: OnrampCheckoutCallback? = null
    private var onrampSessionClientSecretProvider: (suspend (String) -> String)? = null

    /**
     * Callback invoked when signaling the result of verifying the user's identity.
     */
    fun verifyIdentityCallback(callback: OnrampVerifyIdentityCallback) = apply {
        this.verifyIdentityCallback = callback
    }

    /**
     * Callback invoked when KYC verification was attempted to be completed.
     */
    fun verifyKycCallback(callback: OnrampVerifyKycCallback) = apply {
        this.verifyKycCallback = callback
    }

    /**
     * Callback invoked when a payment method was attempted to be collected.
     */
    fun collectPaymentCallback(callback: OnrampCollectPaymentMethodCallback) = apply {
        this.collectPaymentCallback = callback
    }

    /**
     * Callback invoked when gaining user authorization was attempted.
     */
    fun authorizeCallback(callback: OnrampAuthorizeCallback) = apply {
        this.authorizeCallback = callback
    }

    /**
     * Callback invoked when the checkout process has completed.
     */
    fun checkoutCallback(callback: OnrampCheckoutCallback) = apply {
        this.checkoutCallback = callback
    }

    /**
     * An async closure that calls your backend to perform a checkout.
     *     Your backend should call Stripe's `/v1/crypto/onramp_sessions/:id/checkout` endpoint with the session ID.
     *     The closure should return the onramp session client secret on success, or throw an Error on failure.
     *     This closure may be called twice: once initially, and once more after handling any required authentication.
     *     @param The session ID of the current checkout.
     */
    fun onrampSessionClientSecretProvider(callback: suspend (String) -> String) = apply {
        this.onrampSessionClientSecretProvider = callback
    }

    internal class State(
        val verifyIdentityCallback: OnrampVerifyIdentityCallback,
        val verifyKycCallback: OnrampVerifyKycCallback,
        val collectPaymentCallback: OnrampCollectPaymentMethodCallback,
        val authorizeCallback: OnrampAuthorizeCallback,
        val checkoutCallback: OnrampCheckoutCallback,
        val onrampSessionClientSecretProvider: suspend (String) -> String
    )

    internal fun build(): State {
        return State(
            verifyIdentityCallback = requireNotNull(verifyIdentityCallback) {
                "verifyIdentityCallback must not be null"
            },
            verifyKycCallback = requireNotNull(verifyKycCallback) {
                "verifyKycCallback must not be null"
            },
            collectPaymentCallback = requireNotNull(collectPaymentCallback) {
                "collectPaymentCallback must not be null"
            },
            authorizeCallback = requireNotNull(authorizeCallback) {
                "authorizeCallback must not be null"
            },
            checkoutCallback = requireNotNull(checkoutCallback) {
                "checkoutCallback must not be null"
            },
            onrampSessionClientSecretProvider = requireNotNull(onrampSessionClientSecretProvider) {
                "onrampSessionClientSecretProvider must be not null"
            },
        )
    }
}
