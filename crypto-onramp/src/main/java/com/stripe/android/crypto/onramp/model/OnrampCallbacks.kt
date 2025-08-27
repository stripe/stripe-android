package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import dev.drewhamilton.poko.Poko

/**
 * A structure of callbacks used by the Onramp coordinator.
 */
@Poko
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OnrampCallbacks(
    val authenticateUserCallback: OnrampAuthenticateUserCallback,
    val verifyIdentityCallback: OnrampVerifyIdentityCallback,
    val collectPaymentCallback: OnrampCollectPaymentMethodCallback,
    val authorizeCallback: OnrampAuthorizeCallback,
    val checkoutCallback: OnrampCheckoutCallback
)
