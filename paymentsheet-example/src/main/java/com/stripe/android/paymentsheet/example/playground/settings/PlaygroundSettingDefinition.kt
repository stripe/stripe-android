package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal abstract class PlaygroundSettingDefinition<T>(
    val key: String,
    val displayName: String,
) {
    abstract val defaultValue: T

    abstract val options: List<Option<T>>

    abstract fun convertToString(value: T): String

    abstract fun convertToValue(value: String): T

    open val saveToSharedPreferences: Boolean = true

    open fun configure(
        value: T,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PaymentSheetConfigurationData,
    ) {
    }

    open fun configure(
        value: T,
        checkoutRequestBuilder: CheckoutRequest.Builder,
    ) {
    }

    open fun valueUpdated(value: T, playgroundSettings: PlaygroundSettings) {}

    data class Option<T>(val name: String, val value: T)

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
}
