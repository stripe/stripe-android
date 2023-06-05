package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.input.ImeAction
import com.stripe.android.uicore.R
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.stripe.android.core.R as CoreR

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class PhoneNumberController constructor(
    val initialPhoneNumber: String = "",
    initiallySelectedCountryCode: String? = null,
    overrideCountryCodes: Set<String> = emptySet(),
    override val showOptionalLabel: Boolean = false
) : InputController, SectionFieldComposable {
    override val label = flowOf(CoreR.string.stripe_address_label_phone_number)

    private val _fieldValue = MutableStateFlow(initialPhoneNumber)

    /**
     * Flow of the phone number as input by the user, after filtering.
     */
    override val fieldValue: Flow<String> = _fieldValue

    private val _hasFocus = MutableStateFlow(false)

    private val countryConfig = CountryConfig(
        overrideCountryCodes,
        tinyMode = true,
        expandedLabelMapper = { country ->
            listOfNotNull(
                CountryConfig.countryCodeToEmoji(country.code.value),
                country.name,
                PhoneNumberFormatter.prefixForCountry(country.code.value)
            ).joinToString(" ")
        },
        collapsedLabelMapper = { country ->
            listOfNotNull(
                CountryConfig.countryCodeToEmoji(country.code.value),
                PhoneNumberFormatter.prefixForCountry(country.code.value)?.let { "  $it  " }
            ).joinToString("")
        }
    )
    val countryDropdownController = DropdownFieldController(
        countryConfig,
        initiallySelectedCountryCode
    )

    private val phoneNumberFormatter = MutableStateFlow(
        PhoneNumberFormatter.forCountry(
            countryConfig.countries[countryDropdownController.selectedIndex.value].code.value
        )
    )

    private val phoneNumberMinimumLength = countryDropdownController.selectedIndex.map {
        PhoneNumberFormatter.lengthForCountry(
            countryConfig.countries[it].code.value
        )
    }

    /**
     * Flow of the phone number in the E.164 format.
     */
    override val rawFieldValue = combine(fieldValue, phoneNumberFormatter) { value, formatter ->
        formatter.toE164Format(value)
    }
    override val isComplete = combine(fieldValue, phoneNumberMinimumLength) { value, minLength ->
        value.length >= (minLength ?: 0) || showOptionalLabel
    }
    override val formFieldValue = fieldValue.combine(isComplete) { fieldValue, isComplete ->
        FormFieldEntry(fieldValue, isComplete)
    }

    override val error: Flow<FieldError?> = combine(
        fieldValue,
        isComplete,
        _hasFocus
    ) { value, complete, hasFocus ->
        if (value.isNotBlank() && !complete && !hasFocus) {
            FieldError(R.string.stripe_incomplete_phone_number)
        } else {
            null
        }
    }

    val placeholder = phoneNumberFormatter.map { it.placeholder }

    val visualTransformation = phoneNumberFormatter.map { it.visualTransformation }

    fun getCountryCode() = phoneNumberFormatter.value.countryCode

    fun getE164PhoneNumber(phoneNumber: String) =
        phoneNumberFormatter.value.toE164Format(phoneNumber)

    fun getLocalNumber() = _fieldValue.value.removePrefix(phoneNumberFormatter.value.prefix)

    fun onSelectedCountryIndex(index: Int) = countryConfig.countries[index].takeIf {
        it.code.value != phoneNumberFormatter.value.countryCode
    }?.let {
        phoneNumberFormatter.value =
            PhoneNumberFormatter.forCountry(it.code.value)
    }

    fun onValueChange(displayFormatted: String) {
        _fieldValue.value = phoneNumberFormatter.value.userInputFilter(displayFormatted)
    }

    override fun onRawValueChange(rawValue: String) {
        // any value can be treated the same way since it goes through clean up and formatting
        onValueChange(rawValue)
    }

    fun onFocusChange(newHasFocus: Boolean) {
        _hasFocus.value = newHasFocus
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
            val hasCountryPrefix = initialValue.startsWith("+")

            val formatter = if (initiallySelectedCountryCode == null && hasCountryPrefix) {
                PhoneNumberFormatter.forPrefix(
                    initialValue
                )
            } else if (initiallySelectedCountryCode != null) {
                PhoneNumberFormatter.forCountry(
                    initiallySelectedCountryCode
                )
            } else {
                null
            }

            return if (formatter != null) {
                val prefix = formatter.prefix
                val localNumber = initialValue.removePrefix(prefix)
                // Converting to E164 to get rid of any weird formatting, e.g. leading spaces.
                val e164Number = formatter.toE164Format(localNumber)
                PhoneNumberController(
                    initialPhoneNumber = e164Number.removePrefix(prefix),
                    initiallySelectedCountryCode = formatter.countryCode,
                )
            } else {
                PhoneNumberController(
                    initialPhoneNumber = initialValue,
                    initiallySelectedCountryCode = initiallySelectedCountryCode
                )
            }
        }
    }

    @Composable
    override fun ComposeUI(
        enabled: Boolean,
        field: SectionFieldElement,
        modifier: Modifier,
        hiddenIdentifiers: Set<IdentifierSpec>,
        lastTextFieldIdentifier: IdentifierSpec?,
        nextFocusDirection: FocusDirection,
        previousFocusDirection: FocusDirection
    ) {
        PhoneNumberElementUI(
            enabled,
            this,
            imeAction = if (lastTextFieldIdentifier != field.identifier) {
                ImeAction.Next
            } else {
                ImeAction.Done
            }
        )
    }
}
