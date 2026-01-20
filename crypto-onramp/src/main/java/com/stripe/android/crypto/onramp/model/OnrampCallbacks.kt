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
    val authenticateUserCallback: OnrampAuthenticateUserCallback,

    /**
     * Callback invoked when signaling the result of verifiying the user's identity.
     */
    val verifyIdentityCallback: OnrampVerifyIdentityCallback,

    /**
     * Callback invoked when KYC verification was attempted to be completed.
     */
    val verifyKycCallback: OnrampVerifyKycCallback,

    /**
     * Callback invoked when a payment method was attempted to be collected.
     */
    val collectPaymentCallback: OnrampCollectPaymentMethodCallback,

    /**
     * Callback invoked when gaining user authorization was attempted.
     */
    val authorizeCallback: OnrampAuthorizeCallback,

    /**
     * Callback invoked to when the checkout process has completed.
     */
    val checkoutCallback: OnrampCheckoutCallback
)
