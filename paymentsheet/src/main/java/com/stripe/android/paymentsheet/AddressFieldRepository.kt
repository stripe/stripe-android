package com.stripe.android.paymentsheet

import android.content.Context
import com.stripe.android.paymentsheet.elements.parseAddressesSchema
import com.stripe.android.paymentsheet.elements.transformToSpecFieldList
import com.stripe.android.paymentsheet.forms.transform

enum class AddressFieldRepository {
    INSTANCE;

    private val countryFieldMap = mutableMapOf<String, List<SectionFieldElementType>?>()

    internal fun get(countryCode: String?) = countryCode?.let {
        countryFieldMap[it]
    } ?: countryFieldMap[DEFAULT_COUNTRY_CODE]

    fun init(context: Context) {
        val defaultCountrySections = requireNotNull(
            parseAddressesSchema(
                context,
                "addressinfo/${DEFAULT_COUNTRY_CODE}.json"
            )
                ?.transformToSpecFieldList()
                ?.transform(FocusRequesterCount())
        )

        for (countryCode in supportedCountries) {
            countryFieldMap[countryCode] = requireNotNull(
                parseAddressesSchema(
                    context,
                    "addressinfo/$countryCode.json"
                )
                    ?.transformToSpecFieldList()
                    ?.transform(FocusRequesterCount())
                    ?: defaultCountrySections
            )
        }
    }

    companion object {
        private val DEFAULT_COUNTRY_CODE = "ZZ"

        // Matches the Stripe-js-v3 code: https://git.corp.stripe.com/stripe-internal/stripe-js-v3/blob/e4ae99302dde74ccc6fcabdd1a58193f74e21ebf/src/lib/shared/checkoutSupportedCountries.js
        private val supportedCountries = setOf(
            // @formatter:off
            /* ktlint-disable */
            "AE", "AT", "AU", "BE", "BG", "BR", "CA", "CH", "CI", "CR", "CY", "CZ", "DE", "DK",
            "DO", "EE", "ES", "FI", "FR", "GB", "GI", "GR", "GT", "HK", "HU", "ID", "IE", "IN",
            "IT", "JP", "LI", "LT", "LU", "LV", "MT", "MX", "MY", "NL", "NO", "NZ", "PE", "PH",
            "PL", "PT", "RO", "SE", "SG", "SI", "SK", "SN", "TH", "TT", "US", "UY", "ZZ"
            /* ktlint-enable */
            // @formatter:on
        )
    }

}
