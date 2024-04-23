package com.stripe.android.uicore.address

internal interface AddressSchemaDefinition {
    val countryCode: String
    fun schemaElements(): List<CountryAddressSchema>
}
