package com.stripe.android.paymentsheet

import android.content.Context
import com.stripe.android.paymentsheet.elements.parseAddressesSchema
import com.stripe.android.paymentsheet.elements.transformToSpecFieldList
import com.stripe.android.paymentsheet.forms.transform

enum class AddressFieldRepository {
    INSTANCE;

    private val countryFieldMap = mutableMapOf<String, List<SectionFieldElement>?>()

    internal fun get(countryCode: String?) = countryCode?.let {
        countryFieldMap[it]
    } ?: countryFieldMap["ZZ"]

    fun init(context: Context) {
        // TODO: Supported countries should be shared with the dropdown list of countries
        val defaultCountrySections = requireNotNull(
            parseAddressesSchema(
                context,
                "addressinfo/ZZ.json"
            )?.let {
                it.transformToSpecFieldList()
                    .transform(FocusRequesterCount())
            }
        )

        for (countryCode in supportedCountries) {
            countryFieldMap[countryCode] = requireNotNull(
                parseAddressesSchema(
                    context,
                    "addressinfo/$countryCode.json"
                )?.let {
                    it.transformToSpecFieldList()
                        .transform(FocusRequesterCount())
                } ?: defaultCountrySections
            )
        }
    }

    companion object {

        private val supportedCountries = setOf(
            "AE",
            "AT",
            "AU",
            "BE",
            "BG",
            "BR",
            "CA",
            "CH",
            "CI",
            "CR",
            "CY",
            "CZ",
            "DE",
            "DK",
            "DO",
            "EE",
            "ES",
            "FI",
            "FR",
            "GB",
            "GI",
            "GR",
            "GT",
            "HK",
            "HU",
            "ID",
            "IE",
            "IN",
            "IT",
            "JP",
            "LI",
            "LT",
            "LU",
            "LV",
            "MT",
            "MX",
            "MY",
            "NL",
            "NO",
            "NZ",
            "PE",
            "PH",
            "PL",
            "PT",
            "RO",
            "SE",
            "SG",
            "SI",
            "SK",
            "SN",
            "TH",
            "TT",
            "US",
            "UY",
            "ZZ"
        )
    }
}
