package com.stripe.android.paymentsheet.example.playground.customersheet.settings

import com.stripe.android.paymentsheet.example.playground.customersheet.model.CustomerEphemeralKeyRequest

internal object CustomerSettingsDefinition :
    CustomerSheetPlaygroundSettingDefinition<CustomerType>,
    CustomerSheetPlaygroundSettingDefinition.Saveable<CustomerType>,
    CustomerSheetPlaygroundSettingDefinition.Displayable<CustomerType> {
    override val displayName: String = "Customer"
    override val options: List<CustomerSheetPlaygroundSettingDefinition.Displayable.Option<CustomerType>> =
        listOf(
            CustomerSettingsDefinition.option("New", CustomerType.New),
            CustomerSettingsDefinition.option("Returning", CustomerType.Returning),
        )

    override fun configure(
        value: CustomerType,
        requestBuilder: CustomerEphemeralKeyRequest.Builder,
    ) {
        requestBuilder.customerType(value.value)
    }

    override val key: String = "customer"
    override val defaultValue: CustomerType = CustomerType.New

    override fun convertToValue(value: String): CustomerType {
        val hardcodedCustomerTypes = mapOf(
            CustomerType.New.value to CustomerType.New,
            CustomerType.Returning.value to CustomerType.Returning,
        )
        return if (value.startsWith("cus_")) {
            CustomerType.Existing(value)
        } else if (hardcodedCustomerTypes.containsKey(value)) {
            hardcodedCustomerTypes[value]!!
        } else {
            defaultValue
        }
    }

    override fun convertToString(value: CustomerType): String {
        return value.value
    }
}

sealed class CustomerType(val value: String) {
    data object New : CustomerType("new")

    data object Returning : CustomerType("returning")

    class Existing(val customerId: String) : CustomerType(customerId) {
        override fun toString(): String {
            return customerId
        }
    }
}
