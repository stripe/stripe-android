package com.stripe.android.paymentsheet.example.playground.customersheet.settings

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.customersheet.CustomerSheetPlaygroundState
import com.stripe.android.paymentsheet.example.playground.customersheet.model.CustomerEphemeralKeyRequest

internal interface CustomerSheetPlaygroundSettingDefinition<T> {
    val defaultValue: T

    @OptIn(ExperimentalCustomerSheetApi::class)
    fun configure(
        value: T,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: CustomerSheetPlaygroundState,
        configurationData: CustomerSheetConfigurationData,
    ) {
    }

    fun configure(
        value: T,
        requestBuilder: CustomerEphemeralKeyRequest.Builder,
    ) {
    }

    fun valueUpdated(value: T, playgroundSettings: CustomerSheetPlaygroundSettings) {}

    fun saveable(): Saveable<T>? {
        @Suppress("UNCHECKED_CAST")
        return this as? Saveable<T>?
    }

    fun displayable(): Displayable<T>? {
        return this as? Displayable<T>?
    }

    @OptIn(ExperimentalCustomerSheetApi::class)
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

    interface Displayable<T> : CustomerSheetPlaygroundSettingDefinition<T> {
        val displayName: String
        val options: List<Option<T>>

        fun option(name: String, value: T): Option<T> {
            return Option(name, value)
        }

        data class Option<T>(val name: String, val value: T)
    }
}
