package com.stripe.android.paymentsheet.elements

internal sealed interface DropdownConfig {
    /** This is a label for debug logs **/
    val debugLabel: String

    /** This is the label to describe the field */
    val label: Int

    /** This is the list of displayable items to show in the drop down **/
    fun getDisplayItems(): List<String>

    /**
     * This will convert the field to a raw value to use in the parameter map
     */
    fun convertFromRaw(rawValue: String): String

    /**
     * This will convert from a raw value used in the parameter map to a disiplayValue
     */
    fun convertToRaw(displayName: String): String?
}
