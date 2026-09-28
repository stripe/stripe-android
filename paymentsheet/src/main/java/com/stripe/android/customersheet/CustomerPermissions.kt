package com.stripe.android.customersheet

import com.stripe.android.common.model.PaymentMethodRemovePermission

internal data class CustomerPermissions(
    val removePaymentMethod: PaymentMethodRemovePermission,
    val canRemoveLastPaymentMethod: Boolean,
    val canUpdateCardExpiryAndBillingDetails: Boolean
) {
    val canRemovePaymentMethods: Boolean
        get() = removePaymentMethod == PaymentMethodRemovePermission.Full ||
            removePaymentMethod == PaymentMethodRemovePermission.Partial
}
