package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface DropdownConfig {
    /** This is a label for debug logs **/
    val debugLabel: String

    /** This is the label to describe the field **/
    val label: Int

    /** The list of raw values to use in the parameter map **/
    val rawItems: List<String?>

    /** This is the list of displayable items to show in the drop down **/
    val displayItems: List<String>

    /** Whether the dropdown menu should be shown in a small form when collapsed **/
    val tinyMode: Boolean
        get() = false

    /** The label identifying the selected item used when the dropdown menu is collapsed **/
    fun getSelectedItemLabel(index: Int): String

    /** This will convert from a raw value used in the parameter map to a display value **/
    fun convertFromRaw(rawValue: String): String
}
