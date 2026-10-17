package com.stripe.android.model

fun PaymentMethodCreateParams.Card.copy(
    number: String? = this.number,
    expiryMonth: Int? = this.expiryMonth,
    expiryYear: Int? = this.expiryYear,
    cvc: String? = this.cvc,
    attribution: Set<String>? = this.attribution,
    networks: PaymentMethodCreateParams.Card.Networks? = this.networks,
): PaymentMethodCreateParams.Card {
    return PaymentMethodCreateParams.Card(
        number = number,
        expiryMonth = expiryMonth,
        expiryYear = expiryYear,
        cvc = cvc,
        attribution = attribution,
        networks = networks,
    )
}
