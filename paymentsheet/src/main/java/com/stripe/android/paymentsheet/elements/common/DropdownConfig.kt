package com.stripe.android.paymentsheet.elements.common

internal interface DropdownConfig {
    val debugLabel: String
    val label: Int
    fun getDisplayItems(): List<String>
}