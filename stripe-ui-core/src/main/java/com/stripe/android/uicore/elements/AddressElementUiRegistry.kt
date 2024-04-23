package com.stripe.android.uicore.elements

import com.stripe.android.uicore.address.AddressSchemaRegistry
import com.stripe.android.uicore.address.transformToElementList

internal class AddressElementUiRegistry(schemaRegistry: AddressSchemaRegistry) {
    private val defaultElements = schemaRegistry.defaultSchema.schemaElements().transformToElementList(
        countryCode = schemaRegistry.defaultSchema.countryCode
    )

    private val elements = schemaRegistry.all.mapValues { (countryCode, schema) ->
        schema.schemaElements().transformToElementList(countryCode)
    }

    fun get(countryCode: String?): List<SectionFieldElement>? {
        return if (countryCode != null) {
            elements[countryCode]
        } else {
            defaultElements
        }
    }
}
