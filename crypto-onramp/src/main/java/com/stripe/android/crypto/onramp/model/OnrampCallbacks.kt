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

    private var authenticateUserCallback: OnrampAuthenticateUserCallback? = null
    private var verifyIdentityCallback: OnrampVerifyIdentityCallback? = null
    private var verifyKycCallback: OnrampVerifyKycCallback? = null
    private var collectPaymentCallback: OnrampCollectPaymentMethodCallback? = null
    private var authorizeCallback: OnrampAuthorizeCallback? = null
    private var checkoutCallback: OnrampCheckoutCallback? = null

    /**
     * Callback invoked to authenticate the user before starting the onramp flow.
     */
    fun authenticateUserCallback(callback: OnrampAuthenticateUserCallback) = apply {
        this.authenticateUserCallback = callback
    }

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

    class State(
        internal val authenticateUserCallback: OnrampAuthenticateUserCallback,
        internal val verifyIdentityCallback: OnrampVerifyIdentityCallback,
        internal val verifyKycCallback: OnrampVerifyKycCallback,
        internal val collectPaymentCallback: OnrampCollectPaymentMethodCallback,
        internal val authorizeCallback: OnrampAuthorizeCallback,
        internal val checkoutCallback: OnrampCheckoutCallback,
    )

    fun build(): State {
        return State(
            authenticateUserCallback = requireNotNull(authenticateUserCallback) {
                "authenticateUserCallback must not be null"
            },
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
        )
    }
}
