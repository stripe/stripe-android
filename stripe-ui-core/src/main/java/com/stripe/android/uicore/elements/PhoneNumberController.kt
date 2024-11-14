package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.input.ImeAction
import com.stripe.android.uicore.R
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.stripe.android.core.R as CoreR

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class PhoneNumberController private constructor(
    val initialPhoneNumber: String = "",
    initiallySelectedCountryCode: String? = null,
    overrideCountryCodes: Set<String> = emptySet(),
    override val showOptionalLabel: Boolean = false,
    private val acceptAnyInput: Boolean = false,
) : InputController, SectionFieldComposable {
    override val label = stateFlowOf(CoreR.string.stripe_address_label_phone_number)

    private val _fieldValue = MutableStateFlow(initialPhoneNumber)

    /**
     * Flow of the phone number as input by the user, after filtering.
     */
    override val fieldValue: StateFlow<String> = _fieldValue.asStateFlow()

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

    private val phoneNumberFormatter = countryDropdownController.selectedIndex.mapAsStateFlow {
        PhoneNumberFormatter.forCountry(
            countryConfig.countries[it].code.value
        )
    }

    private val phoneNumberMinimumLength = countryDropdownController.selectedIndex.mapAsStateFlow {
        PhoneNumberFormatter.lengthForCountry(
            countryConfig.countries[it].code.value
        )
    }

    /**
     * Flow of the phone number in the E.164 format.
     */
    override val rawFieldValue = combineAsStateFlow(fieldValue, phoneNumberFormatter) { value, formatter ->
        formatter.toE164Format(value)
    }
    override val isComplete = combineAsStateFlow(fieldValue, phoneNumberMinimumLength) { value, minLength ->
        value.length >= (minLength ?: 0) || acceptAnyInput
    }
    override val formFieldValue = combineAsStateFlow(rawFieldValue, isComplete) { rawFieldValue, isComplete ->
        FormFieldEntry(rawFieldValue, isComplete)
    }

    override val error: StateFlow<FieldError?> = combineAsStateFlow(
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

    val placeholder = phoneNumberFormatter.mapAsStateFlow { it.placeholder }

    val visualTransformation = phoneNumberFormatter.mapAsStateFlow { it.visualTransformation }

    fun getCountryCode() = phoneNumberFormatter.value.countryCode

    fun getE164PhoneNumber(phoneNumber: String) =
        phoneNumberFormatter.value.toE164Format(phoneNumber)

    fun getLocalNumber() = _fieldValue.value.removePrefix(phoneNumberFormatter.value.prefix)

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
            initiallySelectedCountryCode: String? = null,
            overrideCountryCodes: Set<String> = emptySet(),
            showOptionalLabel: Boolean = false,
            acceptAnyInput: Boolean = false
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
                    showOptionalLabel = showOptionalLabel,
                    acceptAnyInput = acceptAnyInput,
                    overrideCountryCodes = overrideCountryCodes,
                )
            } else {
                PhoneNumberController(
                    initialPhoneNumber = initialValue,
                    initiallySelectedCountryCode = initiallySelectedCountryCode,
                    showOptionalLabel = showOptionalLabel,
                    acceptAnyInput = acceptAnyInput,
                    overrideCountryCodes = overrideCountryCodes,
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
