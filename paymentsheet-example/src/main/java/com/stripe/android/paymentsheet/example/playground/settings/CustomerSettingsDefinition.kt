package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CustomerSettingsDefinition :
    PlaygroundSettingDefinition<CustomerSettingsDefinition.CustomerType>(
        key = "customer",
        displayName = "Customer",
    ) {
    override val defaultValue: CustomerType = CustomerType.GUEST
    override val options: List<Option<CustomerType>> = listOf(
        Option("Guest", CustomerType.GUEST),
        Option("New", CustomerType.NEW),
        Option("Returning", CustomerType.RETURNING),
    )

    override fun convertToValue(value: String): CustomerType {
        return CustomerType.values().firstOrNull { it.value == value } ?: defaultValue
    }

    override fun convertToString(value: CustomerType): String {
        return value.value
    }

    override fun configure(
        value: CustomerType,
        checkoutRequestBuilder: CheckoutRequest.Builder,
    ) {
        checkoutRequestBuilder.customer(value.value)
    }

    override fun configure(
        value: CustomerType,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PaymentSheetConfigurationData,
    ) {
        configurationBuilder.customer(playgroundState.customerConfig)
    }

    enum class CustomerType(val value: String) {
        GUEST("guest"),
        NEW("new"),
        RETURNING("returning"),
        SNAPSHOT("snapshot"),
    }
}
