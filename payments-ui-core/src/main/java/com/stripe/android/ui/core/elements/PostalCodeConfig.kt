package com.stripe.android.ui.core.elements

import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.max

internal class PostalCodeConfig(
    @StringRes override val label: Int,
    override val capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    override val keyboard: KeyboardType = KeyboardType.Text,
    override val trailingIcon: MutableStateFlow<TextFieldIcon?> = MutableStateFlow(null),
    country: String
) : TextFieldConfig {
    private val format = CountryPostalFormat.forCountry(country)

    override val debugLabel: String = "postal_code_text"
    override val visualTransformation: VisualTransformation =
        PostalCodeVisualTransformation(format)
    override val loading: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override fun determineState(input: String): TextFieldState = object : TextFieldState {
        override fun shouldShowError(hasFocus: Boolean) = false

        override fun isValid(): Boolean {
            return when (format) {
                is CountryPostalFormat.Other -> input.isNotBlank()
                else ->
                    input.length in format.minimumLength..format.maximumLength &&
                        input.matches(format.regexPattern)
            }
        }

        override fun getError(): FieldError? = null

        override fun isFull(): Boolean = input.length >= format.minimumLength

        override fun isBlank(): Boolean = input.isBlank()
    }

    override fun filter(userTyped: String): String =
        if (
            setOf(KeyboardType.Number, KeyboardType.NumberPassword).contains(keyboard)
        ) {
            userTyped.filter { it.isDigit() }
        } else {
            userTyped
        }.dropLast(max(0, userTyped.length - format.maximumLength))

    override fun convertToRaw(displayName: String) = displayName

    override fun convertFromRaw(rawValue: String) =
        rawValue.replace(Regex("\\s+"), "")

    sealed class CountryPostalFormat(
        val minimumLength: Int,
        val maximumLength: Int,
        val regexPattern: Regex
    ) {
        object CA : CountryPostalFormat(
            minimumLength = 6,
            maximumLength = 6,
            regexPattern = Regex("[a-zA-Z]\\d[a-zA-Z][\\s-]?\\d[a-zA-Z]\\d")
        )

        object US : CountryPostalFormat(
            minimumLength = 5,
            maximumLength = 5,
            regexPattern = Regex("\\d+")
        )

        object Other : CountryPostalFormat(
            minimumLength = 1,
            maximumLength = Int.MAX_VALUE,
            regexPattern = Regex(".*")
        )

        companion object {
            fun forCountry(country: String): CountryPostalFormat {
                return when (country) {
                    "US" -> US
                    "CA" -> CA
                    else -> Other
                }
            }
        }
    }
}
