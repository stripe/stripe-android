package com.stripe.android.uicore.address.schemas

import com.stripe.android.uicore.address.AddressSchemaDefinition
import com.stripe.android.uicore.address.CountryAddressSchema
import com.stripe.android.uicore.address.FieldSchema
import com.stripe.android.uicore.address.FieldType
import com.stripe.android.uicore.address.NameType
import kotlin.String
import kotlin.collections.List

internal object PrAddressSchemaDefinition : AddressSchemaDefinition {
    override val countryCode: String = "PR"

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
        CountryAddressSchema(
            type = FieldType.PostalCode,
            required = true,
            schema = FieldSchema(
                nameType = NameType.Zip,
                isNumeric = false,
            ),
        ),
    )
}
