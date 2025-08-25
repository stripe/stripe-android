package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import dev.drewhamilton.poko.Poko

/**
 * A structure of callbacks used by the Onramp coordinator.
 */
@Poko
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OnrampCallbacks(
    val authenticationCallback: OnrampVerificationCallback,
    val identityVerificationCallback: OnrampIdentityVerificationCallback,
    val selectPaymentCallback: OnrampCollectPaymentCallback,
    val authorizeCallback: OnrampAuthorizeCallback
)
