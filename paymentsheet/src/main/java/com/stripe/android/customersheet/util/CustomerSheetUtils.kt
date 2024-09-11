package com.stripe.android.customersheet.util

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection

internal fun sortPaymentMethods(
    paymentMethods: List<PaymentMethod>,
    selection: PaymentSelection.Saved?,
): List<PaymentMethod> {
    return selection?.let { savedSelection ->
        val selectedPaymentMethod = savedSelection.paymentMethod

        // The order of the payment methods should be selected PM and then any additional PMs
        // The carousel always starts with Add and Google Pay (if enabled)
        paymentMethods.sortedWith { left, right ->
            // We only care to move the selected payment method, all others stay in the
            // order they were before
            when {
                left.id == selectedPaymentMethod.id -> -1
                right.id == selectedPaymentMethod.id -> 1
                else -> 0
            }
        }
    } ?: paymentMethods
}
