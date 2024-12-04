package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CustomerEphemeralKeyRequest

internal object CustomerSettingsDefinition :
    PlaygroundSettingDefinition<CustomerType>,
    PlaygroundSettingDefinition.Saveable<CustomerType>,
    PlaygroundSettingDefinition.Displayable<CustomerType> {
    override val displayName: String = "Customer"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<CustomerType>> {
        val configurableOptions = if (configurationData.integrationType.isPaymentFlow()) {
            listOf(option("Guest", CustomerType.GUEST))
        } else {
            listOf()
        }

        return configurableOptions + listOf(
            option("New", CustomerType.NEW),
            option("Returning", CustomerType.RETURNING),
        )
    }

    override fun configure(
        value: CustomerType,
        checkoutRequestBuilder: CheckoutRequest.Builder,
    ) {
        checkoutRequestBuilder.customer(value.value)
    }

    override fun configure(
        value: CustomerType,
        customerEphemeralKeyRequestBuilder: CustomerEphemeralKeyRequest.Builder
    ) {
        customerEphemeralKeyRequestBuilder.customerType(value.value)
    }

    override fun configure(
        value: CustomerType,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        configurationBuilder.customer(playgroundState.customerConfig)
    }

    @ExperimentalEmbeddedPaymentElementApi
    override fun configure(
        value: CustomerType,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        configurationBuilder.customer(playgroundState.customerConfig)
    }

    override val key: String = "customer"
    override val defaultValue: CustomerType = CustomerType.GUEST

    override fun convertToValue(value: String): CustomerType {
        val hardcodedCustomerTypes = mapOf(
            CustomerType.GUEST.value to CustomerType.GUEST,
            CustomerType.NEW.value to CustomerType.NEW,
            CustomerType.RETURNING.value to CustomerType.RETURNING,
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
    object GUEST : CustomerType("guest")

    object NEW : CustomerType("new")

    object RETURNING : CustomerType("returning")

    class Existing(val customerId: String) : CustomerType(customerId) {
        override fun toString(): String {
            return customerId
        }
    }
}
