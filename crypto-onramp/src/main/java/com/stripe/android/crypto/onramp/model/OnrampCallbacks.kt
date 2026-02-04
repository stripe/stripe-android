package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import dev.drewhamilton.poko.Poko

/**
 * Container for all callbacks required by the Onramp coordinator.
 *
 * This class groups together the set of callbacks that drive each step of the
 * onramp flow, from user authentication through checkout completion.
 *
 * Each callback represents a distinct stage in the onramp process and is
 * invoked by the coordinator at the appropriate time.
 */
@Poko
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OnrampCallbacks(

    /**
     * Callback invoked to authenticate the user before starting the onramp flow.
     */
    internal val authenticateUserCallback: OnrampAuthenticateUserCallback,

    /**
     * Callback invoked when signaling the result of verifying the user's identity.
     */
    internal val verifyIdentityCallback: OnrampVerifyIdentityCallback,

    /**
     * Callback invoked when KYC verification was attempted to be completed.
     */
    internal val verifyKycCallback: OnrampVerifyKycCallback,

    /**
     * Callback invoked when a payment method was attempted to be collected.
     */
    internal val collectPaymentCallback: OnrampCollectPaymentMethodCallback,

    /**
     * Callback invoked when gaining user authorization was attempted.
     */
    internal val authorizeCallback: OnrampAuthorizeCallback,

    /**
     * Callback invoked to when the checkout process has completed.
     */
    internal val checkoutCallback: OnrampCheckoutCallback,

    /**
     * @param onrampSessionClientSecretProvider An async closure that calls your backend to perform a checkout.
     *     Your backend should call Stripe's `/v1/crypto/onramp_sessions/:id/checkout` endpoint with the session ID.
     *     The closure should return the onramp session client secret on success, or throw an Error on failure.
     *     This closure may be called twice: once initially, and once more after handling any required authentication.
     */
    internal val onrampSessionClientSecretProvider: suspend () -> String
)
