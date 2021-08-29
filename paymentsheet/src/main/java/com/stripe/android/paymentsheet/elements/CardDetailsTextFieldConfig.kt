package com.stripe.android.paymentsheet.elements

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.model.CardBrand

internal interface CardDetailsTextFieldConfig {
    val capitalization: KeyboardCapitalization
    val debugLabel: String
    val label: Int
    val keyboard: KeyboardType
    val visualTransformation: VisualTransformation
    fun determineState(brand: CardBrand, number: String): TextFieldState
    fun filter(userTyped: String): String
    fun convertToRaw(displayName: String): String
    fun convertFromRaw(rawValue: String): String
}
