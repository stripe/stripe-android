package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.TextFieldConfig
import com.stripe.android.uicore.elements.TextFieldIcon
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.elements.TextFieldStateConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BacsDebitAccountNumberConfig : TextFieldConfig {
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.None

    override val debugLabel: String = DEBUG_LABEL

    @StringRes
    override val label: Int = R.string.stripe_bacs_account_number

    override val placeHolder: String
        get() = PLACEHOLDER

    override val keyboard: KeyboardType = KeyboardType.NumberPassword

    override val visualTransformation: VisualTransformation = VisualTransformation.None

    override val trailingIcon: StateFlow<TextFieldIcon?> = MutableStateFlow(null)

    override val loading: StateFlow<Boolean> = MutableStateFlow(false)

    override fun determineState(input: String): TextFieldState {
        return when {
            input.isBlank() -> TextFieldStateConstants.Error.Blank
            input.length < LENGTH -> TextFieldStateConstants.Error.Incomplete(
                R.string.stripe_bacs_account_number_incomplete
            )
            else -> TextFieldStateConstants.Valid.Full
        }
    }

    override fun filter(userTyped: String): String {
        return userTyped.filter { character ->
            character.isDigit()
        }.take(LENGTH)
    }

    override fun convertToRaw(displayName: String): String = displayName

    override fun convertFromRaw(rawValue: String): String = rawValue

    private companion object {
        const val LENGTH = 8
        const val DEBUG_LABEL = "bacs_debit_account_number"
        const val PLACEHOLDER = "00012345"
    }
}
