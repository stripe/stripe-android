package com.stripe.android.model

import com.stripe.android.model.parsers.AddressJsonParser
import com.stripe.android.uicore.address.AddressSchemas
import com.stripe.android.uicore.address.CountryAddressSchema
import com.stripe.android.uicore.address.FieldSchema
import com.stripe.android.uicore.address.FieldType
import com.stripe.android.uicore.address.NameType
import org.json.JSONObject

object AddressFixtures {
    @JvmField
    val ADDRESS: Address = AddressJsonParser().parse(
        JSONObject(
            """
        {
            "city": "San Francisco",
            "country": "US",
            "line1": "123 Market St",
            "line2": "#345",
            "postal_code": "94107",
            "state": "CA"
        }
            """.trimIndent()
        )
    )

    val ADDRESS_SCHEMA_ELEMENTS = AddressSchemas(
        schemaMap = mapOf(
            "US" to listOf(
                CountryAddressSchema(
                    type = FieldType.AddressLine1,
                    required = true,
                ),
                CountryAddressSchema(
                    type = FieldType.AddressLine2,
                    required = false,
                ),
                CountryAddressSchema(
                    type = FieldType.Locality,
                    required = true,
                    schema = FieldSchema(
                        nameType = NameType.City,
                    ),
                ),
                CountryAddressSchema(
                    type = FieldType.PostalCode,
                    required = true,
                    schema = FieldSchema(
                        isNumeric = true,
                        nameType = NameType.Zip,
                    ),
                ),
                CountryAddressSchema(
                    type = FieldType.AdministrativeArea,
                    required = true,
                    schema = FieldSchema(
                        nameType = NameType.State,
                    ),
                ),
            ),
            "JP" to listOf(
                CountryAddressSchema(
                    type = FieldType.PostalCode,
                    required = true,
                    schema = FieldSchema(
                        isNumeric = false,
                        nameType = NameType.Postal,
                    ),
                ),
                CountryAddressSchema(
                    type = FieldType.AdministrativeArea,
                    required = true,
                    schema = FieldSchema(
                        nameType = NameType.Perfecture,
                    ),
                ),
                CountryAddressSchema(
                    type = FieldType.AddressLine1,
                    required = true,
                ),
                CountryAddressSchema(
                    type = FieldType.AddressLine2,
                    required = false,
                ),
            )
        ),
        defaultCountryCode = "US",
    )
}
