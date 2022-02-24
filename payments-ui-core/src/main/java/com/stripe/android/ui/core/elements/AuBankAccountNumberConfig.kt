package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.ui.core.R

/**
 * A text field configuration for an AU bank account number
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AuBankAccountNumberConfig : TextFieldConfig {
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.None
    override val debugLabel = "au_bank_account_number"
    override val visualTransformation: VisualTransformation? = null

    @StringRes
    override val label = R.string.account_number
    override val keyboard = KeyboardType.Number

    override fun filter(userTyped: String) =
        userTyped.filter { VALID_INPUT_RANGES.contains(it) }.take(LENGTH)

    override fun convertToRaw(displayName: String) = displayName

    override fun convertFromRaw(rawValue: String) = rawValue

    override fun determineState(input: String): TextFieldState {
        input.ifBlank {
            return TextFieldStateConstants.Error.Blank
        }

        if (input.length < LENGTH) {
            return TextFieldStateConstants.Error.Incomplete(
                R.string.account_number_incomplete
            )
        }

        if (input.length > LENGTH) {
            return TextFieldStateConstants.Error.Incomplete(
                R.string.account_number_invalid
            )
        }

        return TextFieldStateConstants.Valid.Full
    }

    private companion object {
        const val LENGTH = 9
        val VALID_INPUT_RANGES = ('0'..'9')
    }
}
