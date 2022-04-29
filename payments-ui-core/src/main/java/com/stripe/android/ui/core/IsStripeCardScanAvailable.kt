package com.stripe.android.ui.core

internal interface IsStripeCardScanAvailable {
    operator fun invoke(): Boolean
}

internal class DefaultIsStripeCardScanAvailable : IsStripeCardScanAvailable {
    override fun invoke(): Boolean {
        return try {
            Class.forName("com.stripe.android.stripecardscan.cardscan.CardScanSheet")
            true
        } catch (_: Exception) {
            false
        }
    }
}
