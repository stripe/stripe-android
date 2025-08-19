package com.stripe.android.link.utils

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState

/**
 * Determines the best fallback payment selection when Link is cleared (e.g., on logout).
 * Follows the same logic as initial payment selection: default PM > first customer PM.
 *
 * @param customer The customer state containing payment methods and default PM info
 * @param metadata Payment method metadata containing feature flags and configuration
 * @return The best fallback payment selection, or null if no suitable fallback exists
 */
internal fun determineFallbackPaymentSelection(
    customer: CustomerState?,
    metadata: PaymentMethodMetadata
): PaymentSelection? {
    if (customer == null) return null

    // Check if default payment method feature is enabled
    val isDefaultPaymentMethodEnabled = metadata.customerMetadata?.isPaymentMethodSetAsDefaultEnabled ?: false

    return if (isDefaultPaymentMethodEnabled) {
        // Use default payment method if available
        customer.paymentMethods.firstOrNull { paymentMethod ->
            customer.defaultPaymentMethodId != null && paymentMethod.id == customer.defaultPaymentMethodId
        }?.toPaymentSelection()
    } else {
        // Fall back to first customer payment method (most recently used appears first)
        customer.paymentMethods.firstOrNull()?.toPaymentSelection()
    }
}

/**
 * Converts a PaymentMethod to a PaymentSelection.Saved
 */
private fun PaymentMethod.toPaymentSelection(): PaymentSelection.Saved {
    return PaymentSelection.Saved(this)
}
