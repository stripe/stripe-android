package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.elements.TextFieldConfig
import com.stripe.android.uicore.elements.TextFieldIcon
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.elements.TextFieldStateConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BacsDebitSortCodeConfig : TextFieldConfig {
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.None

    override val debugLabel: String = DEBUG_LABEL

    override val label: ResolvableString = resolvableString(R.string.stripe_bacs_sort_code)

    override val placeHolder: String
        get() = PLACEHOLDER

    override val keyboard: KeyboardType = KeyboardType.NumberPassword

    override val visualTransformation: VisualTransformation = BacsDebitSortCodeVisualTransformation

    override val trailingIcon: StateFlow<TextFieldIcon?> = MutableStateFlow(null)

    override val loading: StateFlow<Boolean> = MutableStateFlow(false)

    override fun determineState(input: String): TextFieldState {
        return when {
            input.isBlank() -> TextFieldStateConstants.Error.Blank
            input.length < LENGTH -> TextFieldStateConstants.Error.Incomplete(
                R.string.stripe_bacs_sort_code_incomplete
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
        const val LENGTH = 6
        const val DEBUG_LABEL = "bacs_debit_sort_code"
        const val PLACEHOLDER = "10-80-00"
    }
}
