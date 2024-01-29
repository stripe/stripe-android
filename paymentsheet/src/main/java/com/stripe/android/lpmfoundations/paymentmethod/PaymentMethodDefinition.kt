package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.model.PaymentMethod

internal interface PaymentMethodDefinition {
    /**
     * The payment method type, for example: PaymentMethod.Type.Card, etc.
     */
    val type: PaymentMethod.Type
}
