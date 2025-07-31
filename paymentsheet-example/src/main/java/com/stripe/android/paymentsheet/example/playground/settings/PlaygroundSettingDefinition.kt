package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.elements.customersheet.CustomerSheet
import com.stripe.android.link.LinkController
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.Settings
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
        settings: Settings,
    ) {
        configure(value, configurationBuilder, playgroundState, configurationData)
    }

    fun configure(
        value: T,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PaymentSheetConfigurationData,
    ) {
    }

    fun configure(
        value: T,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: EmbeddedConfigurationData,
    ) {
    }

    fun configure(
        value: T,
        configurationBuilder: LinkController.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: LinkControllerConfigurationData,
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
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.SharedPaymentToken,
        configurationData: PaymentSheetConfigurationData,
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

    /** Performs any side effects necessary to use this setting's value.
     *
     * This is useful if you need to take action for a setting to take effect, e.g. if you need to configure a feature
     * flag based on this setting's value.
     * */
    fun setValue(
        value: T
    ) {
    }

    /** Called whenever the value changes.
     *
     * This is useful for updating the UI or other settings after a setting has changed.
     * */
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
        private val billingDetailsCollectionConfigurationBuilder: BillingDetailsCollectionConfigurationBuilder =
            BillingDetailsCollectionConfigurationBuilder()
    ) {
        // Billing details is a nested configuration, but we have individual settings for it in the
        // UI, this helper keeps all of the configurations, rather than just the most recent.
        fun updateBillingDetails(
            block: BillingDetailsCollectionConfigurationBuilder.() -> Unit
        ) {
            billingDetailsCollectionConfigurationBuilder.apply {
                block()
            }
            configurationBuilder.billingDetailsCollectionConfiguration(
                billingDetailsCollectionConfigurationBuilder.build()
            )
        }
    }

    data class EmbeddedConfigurationData(
        private val configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        private val billingDetailsCollectionConfigurationBuilder: BillingDetailsCollectionConfigurationBuilder =
            BillingDetailsCollectionConfigurationBuilder()
    ) {
        // Billing details is a nested configuration, but we have individual settings for it in the
        // UI, this helper keeps all of the configurations, rather than just the most recent.
        fun updateBillingDetails(
            block: BillingDetailsCollectionConfigurationBuilder.() -> Unit
        ) {
            billingDetailsCollectionConfigurationBuilder.apply {
                block()
            }
            configurationBuilder.billingDetailsCollectionConfiguration(
                billingDetailsCollectionConfigurationBuilder.build()
            )
        }
    }

    data class CustomerSheetConfigurationData(
        private val configurationBuilder: CustomerSheet.Configuration.Builder,
        private val billingDetailsCollectionConfigurationBuilder: BillingDetailsCollectionConfigurationBuilder =
            BillingDetailsCollectionConfigurationBuilder()
    ) {
        // Billing details is a nested configuration, but we have individual settings for it in the
        // UI, this helper keeps all of the configurations, rather than just the most recent.
        fun updateBillingDetails(
            block: BillingDetailsCollectionConfigurationBuilder.() -> Unit
        ) {
            billingDetailsCollectionConfigurationBuilder.apply {
                block()
            }
            configurationBuilder.billingDetailsCollectionConfiguration(
                billingDetailsCollectionConfigurationBuilder.build()
            )
        }
    }

    data class LinkControllerConfigurationData(
        private val configurationBuilder: LinkController.Configuration.Builder,
        private val billingDetailsCollectionConfigurationBuilder: BillingDetailsCollectionConfigurationBuilder =
            BillingDetailsCollectionConfigurationBuilder()
    ) {
        // Billing details is a nested configuration, but we have individual settings for it in the
        // UI, this helper keeps all of the configurations, rather than just the most recent.
        fun updateBillingDetails(
            block: BillingDetailsCollectionConfigurationBuilder.() -> Unit
        ) {
            billingDetailsCollectionConfigurationBuilder.apply {
                block()
            }
            configurationBuilder.billingDetailsCollectionConfiguration(
                billingDetailsCollectionConfigurationBuilder.build()
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
