package com.stripe.android.uicore.address

import com.stripe.android.uicore.elements.SectionFieldElement

internal class AddressElements(
    schemaMap: Map<String, List<CountryAddressSchema>>,
    private val defaultCountryCode: String = "ZZ",
) {
    private val elements = schemaMap.mapValues { (countryCode, schemaList) ->
        schemaList.transformToElementList(countryCode)
    }

    fun get(countryCode: String?): List<SectionFieldElement>? {
        return countryCode?.let {
            elements[it]
        } ?: elements[defaultCountryCode]
    }
}
