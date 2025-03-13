package com.stripe.android.paymentsheet.ui

internal data class CardDetailsEntry(
    val cardBrandChoice: CardBrandChoice,
    val expMonth: Int? = null,
    val expYear: Int? = null,
    val city: String? = null,
    val country: String? = null, // two-character country code
    val line1: String? = null,
    val line2: String? = null,
    val postalCode: String? = null,
    val state: String? = null
)