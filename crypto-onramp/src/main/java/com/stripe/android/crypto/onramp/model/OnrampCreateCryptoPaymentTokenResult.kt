package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface OnrampCreateCryptoPaymentTokenResult {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Completed internal constructor(
        val cryptoPaymentToken: String
    ) : OnrampCreateCryptoPaymentTokenResult

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(
        val error: Throwable
    ) : OnrampCreateCryptoPaymentTokenResult
}
