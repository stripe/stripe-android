package com.stripe.android.link.utils

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState

/**
 * Determines the best fallback payment selection.
 * Follows the same logic as initial payment selection: default PM > first customer PM.
 *
 * @return The best fallback payment selection, or null if no suitable fallback exists
 */
internal fun determineFallbackPaymentSelectionAfterLinkLogout(
    customerState: CustomerState?,
    paymentMethodMetadata: PaymentMethodMetadata,
): PaymentSelection? {
    if (customerState == null) return null

    // Check if default payment method feature is enabled
    val isDefaultPaymentMethodEnabled = paymentMethodMetadata
        .customerMetadata?.isPaymentMethodSetAsDefaultEnabled ?: false

    return if (isDefaultPaymentMethodEnabled) {
        // Use default payment method if available
        customerState.paymentMethods.firstOrNull { paymentMethod ->
            customerState.defaultPaymentMethodId != null && paymentMethod.id == customerState.defaultPaymentMethodId
        }
    } else {
        // Fall back to first customer payment method (most recently used appears first)
        customerState.paymentMethods.firstOrNull()
    }?.let { PaymentSelection.Saved(it) }
}
