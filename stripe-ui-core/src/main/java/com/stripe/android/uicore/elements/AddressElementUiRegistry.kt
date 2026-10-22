package com.stripe.android.uicore.elements

import com.stripe.android.uicore.address.AddressSchemaRegistry
import com.stripe.android.uicore.address.transformToElementList

internal class AddressElementUiRegistry(
    private val createElements: (String?) -> List<SectionFieldElement>?,
) {
    constructor(schemaRegistry: AddressSchemaRegistry) : this(
        createElements = { countryCode ->
            schemaRegistry.get(countryCode)?.transformToElementList(
                countryCode = countryCode ?: schemaRegistry.defaultSchema.countryCode
            )
        }
    )

    private val elements = mutableMapOf<String?, List<SectionFieldElement>>()

    fun get(countryCode: String?): List<SectionFieldElement>? {
        return elements[countryCode] ?: createElements(countryCode)?.also {
            elements[countryCode] = it
        }
    }
}
