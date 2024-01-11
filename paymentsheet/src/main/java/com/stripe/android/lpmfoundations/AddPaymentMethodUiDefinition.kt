package com.stripe.android.lpmfoundations

import com.stripe.android.paymentsheet.PaymentSheet

/**
 * Encapsulates the information to show the information required create the payment method.
 */
internal data class AddPaymentMethodUiDefinition(
    /**
     * Identifier used for saving and restoring.
     */
    val identifier: String,

    /**
     * The information required to show the selection UI.
     */
    val addPaymentMethodSelectorUiDefinition: AddPaymentMethodSelectorUiDefinition,

    /**
     * The list of form field element definitions for adding the local payment method.
     */
    val uiElementsDefinitions: List<UiElementDefinition>,
)

internal enum class ContactInformationCollectionMode {
    Name {
        override fun collectionMode(
            configuration: PaymentSheet.BillingDetailsCollectionConfiguration
        ) = configuration.name
    },
    Phone {
        override fun collectionMode(
            configuration: PaymentSheet.BillingDetailsCollectionConfiguration
        ) = configuration.phone
    },
    Email {
        override fun collectionMode(
            configuration: PaymentSheet.BillingDetailsCollectionConfiguration
        ) = configuration.email
    };

    abstract fun collectionMode(
        configuration: PaymentSheet.BillingDetailsCollectionConfiguration
    ): PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode

    fun isAllowed(configuration: PaymentSheet.BillingDetailsCollectionConfiguration): Boolean {
        val collectionMode = collectionMode(configuration = configuration)
        return collectionMode != PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never
    }

    fun isRequired(configuration: PaymentSheet.BillingDetailsCollectionConfiguration): Boolean {
        val collectionMode = collectionMode(configuration = configuration)
        return collectionMode == PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
    }
}
