package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal interface PlaygroundSettingDefinition<T> {
    val defaultValue: T

    fun configure(
        value: T,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PaymentSheetConfigurationData,
    ) {
    }

    fun configure(
        value: T,
        checkoutRequestBuilder: CheckoutRequest.Builder,
    ) {
    }

    fun valueUpdated(value: T, playgroundSettings: PlaygroundSettings) {}

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

    interface Saveable<T> {
        val key: String
        val defaultValue: T
        fun convertToString(value: T): String
        fun convertToValue(value: String): T
        val saveToSharedPreferences: Boolean
            get() = true
    }

    interface Displayable<T> : PlaygroundSettingDefinition<T> {
        val displayName: String
        val options: List<Option<T>>

        fun option(name: String, value: T): Option<T> {
            return Option(name, value)
        }

        data class Option<T>(val name: String, val value: T)
    }
}
