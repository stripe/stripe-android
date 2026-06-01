package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.model.PaymentMethod

internal object SupportedSavedPaymentMethodTypes {
    // TODO: Consider using a Set<PaymentMethod.Type> instead of a List so that membership
    //  checks via `.contains()` (used in PaymentMethodMetadata and CustomerApiRepository)
    //  are O(1) rather than O(n). The list is short today, but a Set better communicates
    //  the intent (unordered, no duplicates) and is slightly more efficient.
    val all: List<PaymentMethod.Type> = listOf(
            PaymentMethod.Type.Card,
            PaymentMethod.Type.USBankAccount,
            PaymentMethod.Type.SepaDebit,
        )
}
