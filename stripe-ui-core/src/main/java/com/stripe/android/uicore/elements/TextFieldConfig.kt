package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import com.stripe.android.core.strings.ResolvableString
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface TextFieldConfig {
    /** This specifies how the field should be capitalized **/
    val capitalization: KeyboardCapitalization

    /** This is a label for debug logs **/
    val debugLabel: String

    /** This is the label to describe the field */
    val label: Int?

    /** This is the type of keyboard to use for this field */
    val keyboard: KeyboardType

    /** Transformation for changing visual output of the input field. */
    val visualTransformation: VisualTransformation?

    /** Overridden layout direction */
    val layoutDirection: LayoutDirection?
        get() = null

    val trailingIcon: StateFlow<TextFieldIcon?>

    val loading: StateFlow<Boolean>

    val placeHolder: String?
        get() = null

    val shouldAnnounceLabel: Boolean
        get() = true

    val shouldAnnounceFieldValue: Boolean
        get() = true

    val overrideContentDescriptionProvider: ((fieldValue: String) -> ResolvableString)?
        get() = null

    /** This will determine the state of the field based on the text */
    fun determineState(input: String): TextFieldState

    /**
     * This works a little like the input filter, removing pasted characters that are invalid in
     * the case where the keyboard allows more than the allowed characters, or characters are
     * pasted in
     *
     * @return displayable string
     */
    fun filter(userTyped: String): String

    /**
     * This will convert the field to a raw value to use in the parameter map
     */
    fun convertToRaw(displayName: String): String

    /**
     * This will convert from a raw value used in the parameter map to a disiplayValue
     */
    fun convertFromRaw(rawValue: String): String
}
