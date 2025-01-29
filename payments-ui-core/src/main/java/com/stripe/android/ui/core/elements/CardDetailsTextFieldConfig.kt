package com.stripe.android.ui.core.elements

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.model.CardBrand
import com.stripe.android.uicore.elements.TextFieldState

/**
 * This is similar to the [com.stripe.android.uicore.elements.TextFieldConfig],
 * but in order to determine the state the card brand is required.
 */
internal interface CardDetailsTextFieldConfig {
    val capitalization: KeyboardCapitalization
    val debugLabel: String
    val label: Int
    val keyboard: KeyboardType
    fun determineVisualTransformation(number: String, panLength: Int): VisualTransformation
    fun determineState(brand: CardBrand, number: String, numberAllowedDigits: Int): TextFieldState
    fun filter(userTyped: String): String
    fun convertToRaw(displayName: String): String
    fun convertFromRaw(rawValue: String): String
}
