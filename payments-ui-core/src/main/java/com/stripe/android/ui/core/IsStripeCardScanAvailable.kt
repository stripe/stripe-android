package com.stripe.android.ui.core

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface IsStripeCardScanAvailable {
    operator fun invoke(): Boolean
}

internal class DefaultIsStripeCardScanAvailable : IsStripeCardScanAvailable {
    override fun invoke(): Boolean = true
}
