package com.stripe.android.paymentsheet.elements

import androidx.compose.ui.text.input.KeyboardType
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.elements.common.TextFieldConfig
import com.stripe.android.paymentsheet.elements.common.TextFieldElementState

internal class NameConfig : TextFieldConfig {
    override val debugLabel = "name"
    override val label = R.string.address_label_name
    override val keyboard = KeyboardType.Text

    override fun determineState(input: String): TextFieldElementState {
        return when {
            input.isEmpty() -> Invalid.BlankAndRequired
            else -> Valid.Limitless
        }
    }

    override fun filter(userTyped: String) = userTyped.filter { it.isLetter() }

    companion object {
        sealed class Valid : TextFieldElementState.TextFieldElementStateValid() {
            object Limitless : Valid() // no auto-advance
            {
                override fun isFull(): Boolean = false
            }
        }

        sealed class Invalid : TextFieldElementState.TextFieldElementStateInvalid() {
            object BlankAndRequired : Invalid() {
                override fun shouldShowError(hasFocus: Boolean): Boolean = false
                override fun getErrorMessageResId(): Int = R.string.blank_and_required
            }
        }
    }
}