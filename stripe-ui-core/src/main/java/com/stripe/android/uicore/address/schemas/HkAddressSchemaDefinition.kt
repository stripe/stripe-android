package com.stripe.android.uicore.address.schemas

import com.stripe.android.uicore.address.AddressSchemaDefinition
import com.stripe.android.uicore.address.CountryAddressSchema
import com.stripe.android.uicore.address.FieldSchema
import com.stripe.android.uicore.address.FieldType
import com.stripe.android.uicore.address.NameType
import kotlin.String
import kotlin.collections.List

internal object HkAddressSchemaDefinition : AddressSchemaDefinition {
    override val countryCode: String = "HK"

    override fun schemaElements(): List<CountryAddressSchema> = listOf(
        CountryAddressSchema(
            type = FieldType.AdministrativeArea,
            required = true,
            schema = FieldSchema(
                nameType = NameType.Area,
                isNumeric = false,
            ),
        ),
        CountryAddressSchema(
            type = FieldType.Locality,
            required = false,
            schema = FieldSchema(
                nameType = NameType.District,
                isNumeric = false,
            ),
        ),
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
