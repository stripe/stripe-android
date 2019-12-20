package com.stripe.android.view

import java.util.Locale
import java.util.regex.Pattern

/**
 * Validation rules for postal codes
 */
internal class PostalCodeValidator {

    fun isValid(
        postalCode: String,
        countryCode: String?,
        optionalShippingInfoFields: List<String>,
        hiddenShippingInfoFields: List<String>
    ): Boolean {
        if (countryCode == null) {
            return false
        }

        return if (postalCode.isEmpty() &&
            isPostalCodeNotRequired(optionalShippingInfoFields, hiddenShippingInfoFields)) {
            // user has configured postal code as optional or hidden and customer has not inputted
            // a postal code
            true
        } else {
            // if the postal code field is not optional or hidden:
            // 1. if there is a regex for the country code, validate the postal code against it
            // 2. if the country does not use a postal code, treat postal code as valid
            // 3. otherwise, postal code must be not-blank
            POSTAL_CODE_PATTERNS[countryCode]?.matcher(postalCode)?.matches()
                ?: (!CountryUtils.doesCountryUsePostalCode(countryCode) || postalCode.isNotBlank())
        }
    }

    private companion object {
        private val POSTAL_CODE_PATTERNS = mapOf(
            Locale.US.country to
                Pattern.compile("^[0-9]{5}(?:-[0-9]{4})?$"),
            Locale.CANADA.country to
                Pattern.compile("^(?!.*[DFIOQU])[A-VXY][0-9][A-Z] ?[0-9][A-Z][0-9]$")
        )

        private fun isPostalCodeNotRequired(
            optionalShippingInfoFields: List<String>,
            hiddenShippingInfoFields: List<String>
        ): Boolean {
            return optionalShippingInfoFields.contains(
                ShippingInfoWidget.CustomizableShippingField.POSTAL_CODE_FIELD
            ) || hiddenShippingInfoFields.contains(
                ShippingInfoWidget.CustomizableShippingField.POSTAL_CODE_FIELD
            )
        }
    }
}
