package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.ui.core.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class UpiConfig : TextFieldConfig {

    private val upiPattern: Regex by lazy {
        // From: https://stackoverflow.com/questions/55143204/how-to-validate-a-upi-id-using-regex
        "[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}".toRegex()
    }

    @StringRes
    override val label: Int = R.string.upi_id_label

    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.None
    override val debugLabel: String = "upi_id"
    override val keyboard: KeyboardType = KeyboardType.Email
    override val visualTransformation: VisualTransformation? = null
    override val trailingIcon: StateFlow<TextFieldIcon?> = MutableStateFlow(value = null)
    override val loading: StateFlow<Boolean> = MutableStateFlow(value = false)

    override fun determineState(input: String): TextFieldState {
        val isValid = upiPattern.matches(input)

        return if (input.isEmpty()) {
            TextFieldStateConstants.Error.Blank
        } else if (isValid) {
            TextFieldStateConstants.Valid.Limitless
        } else {
            TextFieldStateConstants.Error.Incomplete(errorMessageResId = R.string.invalid_upi_id)
        }
    }

    override fun filter(userTyped: String): String = userTyped.trim()

    override fun convertToRaw(displayName: String): String = displayName

    override fun convertFromRaw(rawValue: String): String = rawValue
}
