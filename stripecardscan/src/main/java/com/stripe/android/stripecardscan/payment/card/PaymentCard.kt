package com.stripe.android.stripecardscan.payment.card

internal data class PaymentCard(
    val pan: String?,
    val expiry: PaymentCardExpiry?,
    val issuer: CardIssuer?,
    val cvc: String?,
    val legalName: String?
)

internal data class PaymentCardExpiry(val day: String?, val month: String, val year: String)
