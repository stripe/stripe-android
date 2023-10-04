package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CustomerSettingsDefinition :
    PlaygroundSettingDefinition<CustomerType>,
    PlaygroundSettingDefinition.Saveable<CustomerType> by EnumSaveable(
        key = "customer",
        values = CustomerType.values(),
        defaultValue = CustomerType.GUEST,
    ),
    PlaygroundSettingDefinition.Displayable<CustomerType> {
    override val displayName: String = "Customer"
    override val options: List<PlaygroundSettingDefinition.Displayable.Option<CustomerType>> =
        listOf(
            option("Guest", CustomerType.GUEST),
            option("New", CustomerType.NEW),
            option("Returning", CustomerType.RETURNING),
        )

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
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        configurationBuilder.customer(playgroundState.customerConfig)
    }
}

enum class CustomerType(override val value: String) : ValueEnum {
    GUEST("guest"),
    NEW("new"),
    RETURNING("returning"),
    SNAPSHOT("snapshot"),
}
