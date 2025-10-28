package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.R
import kotlinx.coroutines.flow.MutableStateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class SimpleTextFieldConfig(
    override val label: ResolvableString,
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    override val keyboard: KeyboardType = KeyboardType.Text,
    override val trailingIcon: MutableStateFlow<TextFieldIcon?> = MutableStateFlow(null),
    override val optional: Boolean = false,
    val allowsEmojis: Boolean = true,
) : TextFieldConfig {
    override val debugLabel: String = "generic_text"
    override val visualTransformation: VisualTransformation? = null
    override val loading: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override fun determineState(input: String): TextFieldState = object : TextFieldState {
        override fun shouldShowError(hasFocus: Boolean, isValidating: Boolean) = getError() != null && isValidating

        override fun isValid(): Boolean {
            return optional && input.isEmpty() || input.isNotBlank()
        }

        override fun getError(): FieldError? = if (isValid()) {
            null
        } else {
            FieldError(R.string.stripe_blank_and_required)
        }

        override fun isFull(): Boolean = false

        override fun isBlank(): Boolean = input.isBlank()
    }

    override fun filter(userTyped: String): String {
        var filtered = if (
            setOf(KeyboardType.Number, KeyboardType.NumberPassword).contains(keyboard)
        ) {
            userTyped.filter { it.isDigit() }
        } else {
            userTyped
        }

        if (!allowsEmojis) {
            filtered = EMOJI_REGEX.replace(filtered, "")
        }

        return filtered
    }

    override fun convertToRaw(displayName: String) = displayName

    override fun convertFromRaw(rawValue: String) = rawValue

    private companion object {
        val EMOJI_REGEX = Regex(
            "(?:" +
                "[#*0-9]\\x{FE0F}?\\x{20E3}" + // Keycap sequences
                "|[\\x{00A9}\\x{00AE}\\x{203C}\\x{2049}\\x{2122}\\x{2139}\\x{2194}-\\x{2199}" +
                "\\x{21A9}-\\x{21AA}\\x{231A}-\\x{231B}\\x{2328}\\x{23CF}\\x{23E9}-\\x{23F3}" +
                "\\x{23F8}-\\x{23FA}\\x{24C2}\\x{25AA}-\\x{25AB}\\x{25B6}\\x{25C0}" +
                "\\x{25FB}-\\x{25FE}\\x{2600}-\\x{27BF}\\x{2934}-\\x{2935}\\x{2B05}-\\x{2B07}" +
                "\\x{2B1B}-\\x{2B1C}\\x{2B50}\\x{2B55}\\x{3030}\\x{303D}\\x{3297}\\x{3299}]" +
                "[\\x{FE0E}\\x{FE0F}]?" + // Symbols with optional variation selectors
                "|[\\x{1F300}-\\x{1F9FF}]" + // Emoji ranges (using codepoint notation)
                "|[\\x{1FA00}-\\x{1FAFF}]" + // Extended emoji
                "|[\\x{FE0E}\\x{FE0F}\\x{200D}]" + // Variation selectors & ZWJ
                ")+"
        )
    }
}
