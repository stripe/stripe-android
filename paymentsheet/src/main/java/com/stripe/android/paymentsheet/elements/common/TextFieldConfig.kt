package com.stripe.android.paymentsheet.elements.common

import androidx.compose.ui.text.input.KeyboardType

internal interface TextFieldConfig {
    val debugLabel: String

    /** This is the label to describe the field */
    val label: Int

    /** This is the type of keyboard to use for this field */
    val keyboard: KeyboardType

    /** This will determine the state of the field based on the text */
    fun determineState(input: String): TextFieldState

    /**
     * This works a little like the input filter, removing pasted characters that are invalid in
     * the case where the keyboard allows more than the allowed characters, or characters are pasted in */
    fun filter(userTyped: String): String
}
