package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.uicore.elements.TextFieldConfig
import com.stripe.android.uicore.elements.TextFieldIcon
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.elements.TextFieldStateConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.stripe.android.R as StripeR

/**
 * A text field configuration for an AU bank account number
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AuBankAccountNumberConfig : TextFieldConfig {
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.None
    override val debugLabel = "au_bank_account_number"
    override val visualTransformation: VisualTransformation? = null

    override val trailingIcon: StateFlow<TextFieldIcon?> = MutableStateFlow(null)
    override val loading: StateFlow<Boolean> = MutableStateFlow(false)

    @StringRes
    override val label = StripeR.string.stripe_becs_widget_account_number
    override val keyboard = KeyboardType.Number

    override fun filter(userTyped: String) =
        userTyped.filter { VALID_INPUT_RANGES.contains(it) }.take(MAXIMUM_LENGTH)

    override fun convertToRaw(displayName: String) = displayName

    override fun convertFromRaw(rawValue: String) = rawValue

    override fun determineState(input: String): TextFieldState {
        input.ifBlank {
            return TextFieldStateConstants.Error.Blank
        }

        if (input.length < MINIMUM_LENGTH) {
            return TextFieldStateConstants.Error.Incomplete(
                StripeR.string.stripe_becs_widget_account_number_incomplete
            )
        }

        if (input.length < MAXIMUM_LENGTH) {
            return TextFieldStateConstants.Valid.Limitless
        }

        return TextFieldStateConstants.Valid.Full
    }

    private companion object {
        const val MINIMUM_LENGTH = 4
        const val MAXIMUM_LENGTH = 9
        val VALID_INPUT_RANGES = ('0'..'9')
    }
}
