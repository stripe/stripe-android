package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.ElementType

internal sealed interface DropdownConfig {
    val debugLabel: String
    val label: Int
    val elementType: ElementType
    fun getDisplayItems(): List<String>
}
