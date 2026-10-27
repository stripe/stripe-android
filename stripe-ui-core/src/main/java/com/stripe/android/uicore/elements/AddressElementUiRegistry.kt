package com.stripe.android.uicore.elements

import com.stripe.android.uicore.address.AddressSchemaDefinition
import com.stripe.android.uicore.address.AddressSchemaRegistry
import com.stripe.android.uicore.address.transformToElementList

internal class AddressElementUiRegistry(
    private val schemaForCountry: (String?) -> AddressSchemaDefinition?,
) {
    constructor(schemaRegistry: AddressSchemaRegistry) : this(
        schemaForCountry = schemaRegistry::getDefinition,
    )

    private val elements = mutableMapOf<String?, List<SectionFieldElement>>()

    @Synchronized
    fun get(countryCode: String?): List<SectionFieldElement>? {
        return elements.getOrPut(countryCode) {
            val schema = schemaForCountry(countryCode) ?: return null
            schema.schemaElements().transformToElementList(countryCode ?: "ZZ")
        }
    }
}
