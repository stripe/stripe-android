package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.model.PaymentMethod

internal object SupportedSavedPaymentMethodTypes {
    val all: List<PaymentMethod.Type> = listOf(
            PaymentMethod.Type.Card,
            PaymentMethod.Type.USBankAccount,
            PaymentMethod.Type.SepaDebit,
        )
}