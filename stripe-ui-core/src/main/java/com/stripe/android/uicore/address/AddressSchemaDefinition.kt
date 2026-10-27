package com.stripe.android.uicore.address

internal interface AddressSchemaDefinition {
    fun schemaElements(): List<CountryAddressSchema>
}
