package com.stripe.android.link.utils

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.PaymentSheetState

/**
 * Determines the best fallback payment selection.
 * Follows the same logic as initial payment selection: default PM > first customer PM.
 *
 * @return The best fallback payment selection, or null if no suitable fallback exists
 */
internal fun PaymentSheetState.Full.determineFallbackPaymentSelection(): PaymentSelection? {
    if (customer == null) return null

    // Check if default payment method feature is enabled
    val isDefaultPaymentMethodEnabled = paymentMethodMetadata
        .customerMetadata?.isPaymentMethodSetAsDefaultEnabled ?: false

    return if (isDefaultPaymentMethodEnabled) {
        // Use default payment method if available
        customer.paymentMethods.firstOrNull { paymentMethod ->
            customer.defaultPaymentMethodId != null && paymentMethod.id == customer.defaultPaymentMethodId
        }
    } else {
        // Fall back to first customer payment method (most recently used appears first)
        customer.paymentMethods.firstOrNull()
    }?.let { PaymentSelection.Saved(it) }
}
