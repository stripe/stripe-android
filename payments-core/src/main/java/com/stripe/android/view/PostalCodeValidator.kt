package com.stripe.android.view

import com.stripe.android.core.model.CountryUtils
import java.util.Locale
import java.util.regex.Pattern

internal object PostalCodeValidator {

    /**
     * 1. if there is a regex for the country code, validate the postal code against it
     * 2. if the country does not use a postal code, treat postal code as valid
     * 3. otherwise, postal code must be not-blank
     */
    fun isValid(
        postalCode: String,
        countryCode: String
    ): Boolean {
        return POSTAL_CODE_PATTERNS[countryCode]?.matcher(postalCode)?.matches()
            ?: (!CountryUtils.doesCountryUsePostalCode(countryCode) || postalCode.isNotBlank())
    }

    internal fun isValid(
        postalCode: String,
        countryCode: String?,
        optionalShippingInfoFields: List<ShippingInfoWidget.CustomizableShippingField>,
        hiddenShippingInfoFields: List<ShippingInfoWidget.CustomizableShippingField>
    ): Boolean {
        if (countryCode == null) {
            return false
        }

        return if (postalCode.isBlank() &&
            isPostalCodeNotRequired(optionalShippingInfoFields, hiddenShippingInfoFields)
        ) {
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

    private const val usZipCodeRegex = "^[0-9]{5}(?:-[0-9]{4})?$"

    private val canadaPostalCodeRegex = """
            ^[ABCEGHJKLMNPRSTVXY][0-9][ABCEGHJKLMNPRSTVWXYZ]\s?[0-9][ABCEGHJKLMNPRSTVWXYZ][0-9]$
        """.trimIndent()

    private val POSTAL_CODE_PATTERNS = mapOf(
        Locale.US.country to Pattern.compile(usZipCodeRegex),
        Locale.CANADA.country to Pattern.compile(canadaPostalCodeRegex, Pattern.CASE_INSENSITIVE),
    )

    private fun isPostalCodeNotRequired(
        optionalShippingInfoFields: List<ShippingInfoWidget.CustomizableShippingField>,
        hiddenShippingInfoFields: List<ShippingInfoWidget.CustomizableShippingField>
    ): Boolean {
        return optionalShippingInfoFields.contains(
            ShippingInfoWidget.CustomizableShippingField.PostalCode
        ) || hiddenShippingInfoFields.contains(
            ShippingInfoWidget.CustomizableShippingField.PostalCode
        )
    }
}
