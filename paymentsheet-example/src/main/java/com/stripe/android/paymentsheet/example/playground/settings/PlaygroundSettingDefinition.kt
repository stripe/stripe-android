package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CustomerEphemeralKeyRequest

internal interface PlaygroundSettingDefinition<T> {
    val defaultValue: T

    fun configure(
        value: T,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PaymentSheetConfigurationData,
    ) {
    }

    @ExperimentalEmbeddedPaymentElementApi
    fun configure(
        value: T,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: EmbeddedConfigurationData,
    ) {
    }

    fun configure(
        value: T,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Customer,
        configurationData: CustomerSheetConfigurationData,
    ) {
    }

    fun configure(
        value: T,
        checkoutRequestBuilder: CheckoutRequest.Builder,
    ) {
    }

    fun configure(
        value: T,
        customerEphemeralKeyRequestBuilder: CustomerEphemeralKeyRequest.Builder,
    ) {
    }

    fun valueUpdated(value: T, playgroundSettings: PlaygroundSettings) {}

    fun applicable(configurationData: PlaygroundConfigurationData): Boolean = true

    fun saveable(): Saveable<T>? {
        @Suppress("UNCHECKED_CAST")
        return this as? Saveable<T>?
    }

    fun displayable(): Displayable<T>? {
        return this as? Displayable<T>?
    }

    data class PaymentSheetConfigurationData(
        private val configurationBuilder: PaymentSheet.Configuration.Builder,
        var billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
            PaymentSheet.BillingDetailsCollectionConfiguration()
    ) {
        // Billing details is a nested configuration, but we have individual settings for it in the
        // UI, this helper keeps all of the configurations, rather than just the most recent.
        fun updateBillingDetails(
            block: PaymentSheet.BillingDetailsCollectionConfiguration.() ->
            PaymentSheet.BillingDetailsCollectionConfiguration
        ) {
            billingDetailsCollectionConfiguration.apply {
                billingDetailsCollectionConfiguration = block()
            }
            configurationBuilder.billingDetailsCollectionConfiguration(
                billingDetailsCollectionConfiguration
            )
        }
    }

    @ExperimentalEmbeddedPaymentElementApi
    data class EmbeddedConfigurationData(
        private val configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        var billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
            PaymentSheet.BillingDetailsCollectionConfiguration()
    ) {
        // Billing details is a nested configuration, but we have individual settings for it in the
        // UI, this helper keeps all of the configurations, rather than just the most recent.
        fun updateBillingDetails(
            block: PaymentSheet.BillingDetailsCollectionConfiguration.() ->
            PaymentSheet.BillingDetailsCollectionConfiguration
        ) {
            billingDetailsCollectionConfiguration.apply {
                billingDetailsCollectionConfiguration = block()
            }
            configurationBuilder.billingDetailsCollectionConfiguration(
                billingDetailsCollectionConfiguration
            )
        }
    }

    data class CustomerSheetConfigurationData(
        private val configurationBuilder: CustomerSheet.Configuration.Builder,
        var billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
            PaymentSheet.BillingDetailsCollectionConfiguration()
    ) {
        // Billing details is a nested configuration, but we have individual settings for it in the
        // UI, this helper keeps all of the configurations, rather than just the most recent.
        fun updateBillingDetails(
            block: PaymentSheet.BillingDetailsCollectionConfiguration.() ->
            PaymentSheet.BillingDetailsCollectionConfiguration
        ) {
            billingDetailsCollectionConfiguration.apply {
                billingDetailsCollectionConfiguration = block()
            }
            configurationBuilder.billingDetailsCollectionConfiguration(
                billingDetailsCollectionConfiguration
            )
        }
    }

    interface Saveable<T> {
        val key: String
        val defaultValue: T
        fun convertToString(value: T): String
        fun convertToValue(value: String): T
        val saveToSharedPreferences: Boolean
            get() = true
    }

    sealed interface Displayable<T> : PlaygroundSettingDefinition<T> {
        val displayName: String

        fun createOptions(configurationData: PlaygroundConfigurationData): List<Option<T>>

        fun option(name: String, value: T): Option<T> {
            return Option(name, value)
        }

        data class Option<T>(val name: String, val value: T)
    }
}
