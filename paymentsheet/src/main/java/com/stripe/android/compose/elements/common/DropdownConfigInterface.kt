package com.stripe.android.compose.elements.common

internal interface DropdownConfigInterface {
    val debugLabel: String

    /** This is the label to describe the element */
    val label: Int

    fun convertToDisplay(paramFormatted: String?): String = paramFormatted ?: ""
    fun convertToPaymentMethodParam(displayFormatted: String): String? {
        return displayFormatted
    }

    fun getItems(): List<String>
}