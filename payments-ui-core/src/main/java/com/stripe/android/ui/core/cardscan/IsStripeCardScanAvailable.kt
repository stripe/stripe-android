package com.stripe.android.ui.core.cardscan

import androidx.annotation.RestrictTo
import javax.inject.Inject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface IsStripeCardScanAvailable {
    operator fun invoke(): Boolean
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultIsStripeCardScanAvailable @Inject constructor() : IsStripeCardScanAvailable {
    override fun invoke(): Boolean {
        return try {
            Class.forName("com.stripe.android.stripecardscan.cardscan.CardScanSheet")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }
}
