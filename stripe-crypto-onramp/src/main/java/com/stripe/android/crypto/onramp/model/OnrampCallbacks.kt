package com.stripe.android.crypto.onramp.model

import dev.drewhamilton.poko.Poko

/**
 * A structure of callbacks used by the Onramp coordinator.
 *
 * @property linkLookupCallback A callback for when a link user lookup has been completed.
 */
@Poko
class OnrampCallbacks(
    val linkLookupCallback: OnrampLinkLookupCallback,
    val authenticationCallback: OnrampVerificationCallback,
    val registerUserCallback: OnrampRegisterUserCallback
)
