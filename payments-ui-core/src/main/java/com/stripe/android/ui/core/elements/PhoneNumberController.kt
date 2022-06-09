package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.core.os.LocaleListCompat
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class PhoneNumberController internal constructor(
    val initialPhoneNumber: String = "",
    initiallySelectedCountryCode: String? = null,
    overrideCountryCodes: Set<String> = emptySet()
) : InputController {
    override val label = flowOf(R.string.address_label_phone_number)

    private val _fieldValue = MutableStateFlow(initialPhoneNumber)

    /**
     * Flow of the phone number as input by the user, after filtering.
     */
    override val fieldValue: Flow<String> = _fieldValue

    private val countryConfig = CountryConfig(overrideCountryCodes, flagMode = true)
    val countryDropdownController = DropdownFieldController(
        countryConfig,
        initiallySelectedCountryCode
    )

    private val phoneNumberFormatter = MutableStateFlow(
        PhoneNumberFormatter.forCountry(
            countryConfig.countries[countryDropdownController.selectedIndex.value].code.value
        )
    )

    /**
     * Flow of the phone number in the E.164 format.
     */
    override val rawFieldValue = combine(fieldValue, phoneNumberFormatter) { value, formatter ->
        formatter.toE164Format(value)
    }
    override val isComplete = fieldValue.map { it.isNotBlank() }
    override val showOptionalLabel = false
    override val formFieldValue = fieldValue.map {
        FormFieldEntry(it, it.isNotBlank())
    }

    override val error: Flow<FieldError?> = flowOf(null)

    internal val placeholder = phoneNumberFormatter.map { it.placeholder }
    internal val prefix = phoneNumberFormatter.map { it.prefix }
    internal val visualTransformation = phoneNumberFormatter.map { it.visualTransformation }

    fun getCountryCode() = phoneNumberFormatter.value.countryCode

    fun getE164PhoneNumber(phoneNumber: String) =
        phoneNumberFormatter.value.toE164Format(phoneNumber)

    fun onSelectedCountryIndex(index: Int) = countryConfig.countries[index].takeIf {
        it.code.value != phoneNumberFormatter.value.countryCode
    }?.let {
        phoneNumberFormatter.value = PhoneNumberFormatter.forCountry(it.code.value)
    }

    fun onValueChange(displayFormatted: String) {
        _fieldValue.value = phoneNumberFormatter.value.userInputFilter(displayFormatted)
    }

    override fun onRawValueChange(rawValue: String) {
        // any value can be treated the same way since it goes through clean up and formatting
        onValueChange(rawValue)
    }

    companion object {
        /**
         * Instantiate a [PhoneNumberController] with the given initial values.
         * If [initialValue] is in the E.164 format, try to find the most likely country code based
         * on the prefix and the device's locales list.
         */
        fun createPhoneNumberController(
            initialValue: String = "",
            initiallySelectedCountryCode: String? = null
        ): PhoneNumberController {
            // Find the regions that match the phone number prefix, then pick the top match from the
            // device's locales
            if (initiallySelectedCountryCode == null && initialValue.startsWith("+")) {
                var charIndex = 1
                while (charIndex < initialValue.length - 1 && charIndex < 4) {
                    charIndex++
                    PhoneNumberFormatter.findBestCountryForPrefix(
                        initialValue.substring(0, charIndex), LocaleListCompat.getAdjustedDefault()
                    )?.let {
                        return PhoneNumberController(
                            initialPhoneNumber = initialValue.substring(charIndex),
                            initiallySelectedCountryCode = it
                        )
                    }
                }
            }

            // Clean up if initial country is set and country prefix is in initial phone number
            if (initiallySelectedCountryCode != null && initialValue.startsWith("+")) {
                val prefix = PhoneNumberFormatter.forCountry(initiallySelectedCountryCode).prefix
                return PhoneNumberController(
                    initialPhoneNumber = initialValue.removePrefix(prefix),
                    initiallySelectedCountryCode = initiallySelectedCountryCode
                )
            }

            return PhoneNumberController(
                initialPhoneNumber = initialValue,
                initiallySelectedCountryCode = initiallySelectedCountryCode
            )
        }
    }
}
