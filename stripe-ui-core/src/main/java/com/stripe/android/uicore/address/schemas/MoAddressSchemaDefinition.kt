package com.stripe.android.uicore.address.schemas

import com.stripe.android.uicore.address.AddressSchemaDefinition
import com.stripe.android.uicore.address.CountryAddressSchema
import com.stripe.android.uicore.address.FieldType
import kotlin.String
import kotlin.collections.List

internal object MoAddressSchemaDefinition : AddressSchemaDefinition {
    override val countryCode: String = "MO"

    override fun schemaElements(): List<CountryAddressSchema> = listOf(
        CountryAddressSchema(
            type = FieldType.AddressLine1,
            required = true,
            schema = null,
        ),
        CountryAddressSchema(
            type = FieldType.AddressLine2,
            required = false,
            schema = null,
        ),
    )
}
