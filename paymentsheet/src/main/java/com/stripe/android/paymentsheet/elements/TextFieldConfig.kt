package com.stripe.android.paymentsheet.elements

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.flow.MutableStateFlow

internal sealed interface TextFieldConfig {
    /** This specifies how the field should be capitalized **/
    val capitalization: KeyboardCapitalization

    /** This is a label for debug logs **/
    val debugLabel: String

    /** This is the label to describe the field */
    val label: Int

    /** This is the type of keyboard to use for this field */
    val keyboard: KeyboardType

    /** Transformation for changing visual output of the input field. */
    val visualTransformation: VisualTransformation?

    val trailingIcon: MutableStateFlow<TextFieldIcon?>

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
