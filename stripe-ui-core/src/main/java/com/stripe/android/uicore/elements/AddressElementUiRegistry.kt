package com.stripe.android.uicore.elements

import com.stripe.android.uicore.address.AddressSchemaDefinition
import com.stripe.android.uicore.address.AddressSchemaRegistry
import com.stripe.android.uicore.address.transformToElementList

internal class AddressElementUiRegistry(
    private val schemas: Map<String, AddressSchemaDefinition>,
    private val defaultSchema: AddressSchemaDefinition,
) {
    constructor(schemaRegistry: AddressSchemaRegistry) : this(
        schemas = schemaRegistry.all,
        defaultSchema = schemaRegistry.defaultSchema,
    )

    private val elements = mutableMapOf<String?, List<SectionFieldElement>>()

    @Synchronized
    fun get(countryCode: String?): List<SectionFieldElement>? {
        return elements.getOrPut(countryCode) {
            val schema = if (countryCode == null) defaultSchema else schemas[countryCode] ?: return null
            schema.schemaElements().transformToElementList(countryCode ?: schema.countryCode)
        }
    }
}
