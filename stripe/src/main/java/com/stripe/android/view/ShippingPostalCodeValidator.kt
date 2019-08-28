package com.stripe.android.view

import java.util.Locale
import java.util.regex.Pattern

/**
 * Validation rules for postal code for use in [ShippingInfoWidget]
 */
internal class ShippingPostalCodeValidator {

    fun isValid(
        postalCode: String,
        countryCode: String,
        optionalShippingInfoFields: List<String>,
        hiddenShippingInfoFields: List<String>
    ): Boolean {
        val postalCodePattern = POSTAL_CODE_PATTERNS[countryCode]
        return if (postalCode.isEmpty() &&
            isPostalCodeOptional(optionalShippingInfoFields, hiddenShippingInfoFields)) {
            true
        } else postalCodePattern?.matcher(postalCode)?.matches()
            ?: if (CountryUtils.doesCountryUsePostalCode(countryCode)) {
                postalCode.isNotEmpty()
            } else {
                true
            }
    }

    companion object {
        private val POSTAL_CODE_PATTERNS = mapOf(
            Locale.US.country to
                Pattern.compile("^[0-9]{5}(?:-[0-9]{4})?$"),
            Locale.CANADA.country to
                Pattern.compile("^(?!.*[DFIOQU])[A-VXY][0-9][A-Z] ?[0-9][A-Z][0-9]$"),
            Locale.UK.country to
                Pattern.compile("^[A-Z]{1,2}[0-9R][0-9A-Z]? [0-9][ABD-HJLNP-UW-Z]{2}$")
        )

        private fun isPostalCodeOptional(
            optionalShippingInfoFields: List<String>,
            hiddenShippingInfoFields: List<String>
        ): Boolean {
            return optionalShippingInfoFields
                .contains(ShippingInfoWidget.CustomizableShippingField.POSTAL_CODE_FIELD) ||
                hiddenShippingInfoFields
                .contains(ShippingInfoWidget.CustomizableShippingField.POSTAL_CODE_FIELD)
        }
    }
}
