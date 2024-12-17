package com.stripe.android.customersheet

internal data class CustomerPermissions(
    val canRemovePaymentMethods: Boolean,
    val canRemoveLastPaymentMethod: Boolean,
)
