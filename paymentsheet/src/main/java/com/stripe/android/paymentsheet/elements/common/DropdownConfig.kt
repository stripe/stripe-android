package com.stripe.android.paymentsheet.elements.common

internal interface DropdownConfig {
    val debugLabel: String

    /** This is the label to describe the element */
    val label: Int

    fun getDisplayItems(): List<String>
    fun getPaymentMethodParams(): List<String>

}