package com.stripe.android.crypto.onramp.model

import dev.drewhamilton.poko.Poko

/**
 * A structure of callbacks used by the Onramp coordinator.
 */
@Poko
class OnrampCallbacks(
    val authenticationCallback: OnrampVerificationCallback,
    val registerUserCallback: OnrampRegisterUserCallback
)
