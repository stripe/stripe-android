package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class PhoneNumberController internal constructor(
    initialPhoneNumber: String = "",
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
        fun createPhoneNumberController(
            initialValue: String = "",
            initiallySelectedCountryCode: String? = null
        ) = PhoneNumberController(
            initialPhoneNumber = initialValue,
            initiallySelectedCountryCode = initiallySelectedCountryCode
        )
    }
}
