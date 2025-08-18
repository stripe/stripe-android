package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface DropdownConfig {
    /** This is a label for debug logs **/
    val debugLabel: String

    /** This is the label to describe the field **/
    val label: ResolvableString

    /** The list of raw values to use in the parameter map **/
    val rawItems: List<String?>

    /** This is the list of displayable items to show in the drop down **/
    val displayItems: List<String>

    /** Whether the dropdown menu should be shown in a small form when collapsed **/
    val mode: Mode
        get() = Mode.Full()

    /** Whether the dropdown should be disabled when there is only one single item **/
    val disableDropdownWithSingleElement: Boolean
        get() = false

    /** The label identifying the selected item used when the dropdown menu is collapsed **/
    fun getSelectedItemLabel(index: Int): String

    /** This will convert from a raw value used in the parameter map to a display value **/
    fun convertFromRaw(rawValue: String): String

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface Mode {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data object Condensed : Mode

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Full(val selectsFirstOptionAsDefault: Boolean = true) : Mode
    }
}
