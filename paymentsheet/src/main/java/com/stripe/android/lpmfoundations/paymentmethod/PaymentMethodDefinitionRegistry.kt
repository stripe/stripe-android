package com.stripe.android.lpmfoundations.paymentmethod

/**
 * The registry of all [PaymentMethodDefinition]s.
 */
internal object PaymentMethodDefinitionRegistry {
    val all: Set<PaymentMethodDefinition> = setOf(
        PayPalPaymentMethodDefinition,
    )
}
