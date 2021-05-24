package com.stripe.android.compose.elements.common

import androidx.compose.ui.text.input.KeyboardType

internal interface TextFieldConfigInterface {
    val debugLabel: String

    /** This is the label to describe the element */
    val label: Int

    /** This is the type of keyboard to use for this element */
    val keyboard: KeyboardType

    /** This will determine the state of the element based on the text */
    fun determineState(paramFormatted: String?): TextFieldElementState

    /** This will determine if the element is in an error state based on the current focus state */
    fun shouldShowError(elementState: TextFieldElementState, hasFocus: Boolean): Boolean

    /** This works a little like the input filter, removing pasted characters that are invalid */
    fun filter(userTyped: String): String
}