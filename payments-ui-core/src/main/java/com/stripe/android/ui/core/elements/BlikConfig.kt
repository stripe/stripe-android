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

private const val BLIK_MAX_LENGTH = 6

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BlikConfig : TextFieldConfig {

    private val blikPattern: Regex by lazy {
        "^[0-9]{6}\$".toRegex()
    }

    @StringRes
    override val label: Int = R.string.stripe_blik_code

    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.None
    override val debugLabel: String = "blik_code"
    override val keyboard: KeyboardType = KeyboardType.Number
    override val visualTransformation: VisualTransformation? = null
    override val trailingIcon: StateFlow<TextFieldIcon?> = MutableStateFlow(value = null)
    override val loading: StateFlow<Boolean> = MutableStateFlow(value = false)

    override fun determineState(input: String): TextFieldState {
        val isValid = blikPattern.matches(input)
        return if (input.isEmpty()) {
            TextFieldStateConstants.Error.Blank
        } else if (isValid) {
            TextFieldStateConstants.Valid.Limitless
        } else if (!input.all { it.isDigit() }) {
            TextFieldStateConstants.Error.Invalid(
                errorMessageResId = R.string.stripe_invalid_blik_code
            )
        } else if (input.length < BLIK_MAX_LENGTH) {
            TextFieldStateConstants.Error.Incomplete(
                errorMessageResId = R.string.stripe_incomplete_blik_code
            )
        } else {
            TextFieldStateConstants.Error.Invalid(
                errorMessageResId = R.string.stripe_invalid_blik_code
            )
        }
    }

    override fun filter(userTyped: String) =
        userTyped.filter { it.isDigit() }.take(BLIK_MAX_LENGTH)

    override fun convertToRaw(displayName: String): String = displayName

    override fun convertFromRaw(rawValue: String): String = rawValue
}
