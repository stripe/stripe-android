package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.stripe.android.uicore.R
import kotlinx.coroutines.flow.MutableStateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PostalCodeConfig(
    @StringRes override val label: Int,
    override val trailingIcon: MutableStateFlow<TextFieldIcon?> = MutableStateFlow(null),
    private val country: String
) : TextFieldConfig {
    private val format = CountryPostalFormat.forCountry(country)

    override val capitalization: KeyboardCapitalization = when (format) {
        CountryPostalFormat.US -> KeyboardCapitalization.None
        CountryPostalFormat.CA,
        CountryPostalFormat.GB,
        CountryPostalFormat.Other -> KeyboardCapitalization.Characters
    }

    override val keyboard: KeyboardType = when (format) {
        CountryPostalFormat.US -> KeyboardType.NumberPassword
        CountryPostalFormat.CA,
        CountryPostalFormat.GB,
        CountryPostalFormat.Other -> KeyboardType.Text
    }

    override val debugLabel: String = "postal_code_text"
    override val visualTransformation: VisualTransformation =
        PostalCodeVisualTransformation(format)
    override val loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val shouldAnnounceFieldValue = false

    override fun determineState(input: String): TextFieldState = object : TextFieldState {
        override fun shouldShowError(hasFocus: Boolean) = getError() != null && !hasFocus

        override fun isValid(): Boolean {
            return when (format) {
                is CountryPostalFormat.Other -> input.isNotBlank()
                else -> {
                    input.length in format.minimumLength..format.maximumLength &&
                        input.matches(format.regexPattern)
                }
            }
        }

        override fun getError(): FieldError? {
            return when {
                input.isNotBlank() && !isValid() && country == "US" -> {
                    FieldError(R.string.stripe_address_zip_invalid)
                }
                input.isNotBlank() && !isValid() -> {
                    FieldError(R.string.stripe_address_zip_postal_invalid)
                }
                else -> null
            }
        }

        override fun isFull(): Boolean = input.length >= format.maximumLength

        override fun isBlank(): Boolean = input.isBlank()
    }

    override fun filter(userTyped: String): String {
        return when (format) {
            CountryPostalFormat.US -> userTyped.filter { it.isDigit() }
            CountryPostalFormat.CA,
            CountryPostalFormat.GB -> userTyped.filter { it.isLetterOrDigit() }.uppercase()
            CountryPostalFormat.Other -> userTyped
        }.take(format.maximumLength)
    }

    override fun convertToRaw(displayName: String) = displayName

    override fun convertFromRaw(rawValue: String) =
        rawValue.replace(Regex("\\s+"), "")

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed class CountryPostalFormat(
        val minimumLength: Int,
        val maximumLength: Int,
        val regexPattern: Regex
    ) {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        object CA : CountryPostalFormat(
            minimumLength = 6,
            maximumLength = 6,
            regexPattern = Regex("[a-zA-Z]\\d[a-zA-Z][\\s-]?\\d[a-zA-Z]\\d")
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        object GB : CountryPostalFormat(
            minimumLength = 5,
            maximumLength = 7,
            regexPattern = Regex("^[A-Za-z][A-Za-z0-9]*(?: [A-Za-z0-9]*)?\$")
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        object US : CountryPostalFormat(
            minimumLength = 5,
            maximumLength = 5,
            regexPattern = Regex("\\d+")
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        object Other : CountryPostalFormat(
            minimumLength = 1,
            maximumLength = Int.MAX_VALUE,
            regexPattern = Regex(".*")
        )

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        companion object {
            fun forCountry(country: String): CountryPostalFormat {
                return when (country) {
                    "US" -> US
                    "CA" -> CA
                    "GB" -> GB
                    else -> Other
                }
            }
        }
    }
}
