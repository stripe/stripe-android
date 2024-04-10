package com.stripe.android.lpmfoundations.luxe

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.PhoneSpec
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec

internal enum class ContactInformationCollectionMode {
    Name {
        override fun collectionMode(
            configuration: PaymentSheet.BillingDetailsCollectionConfiguration
        ): PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode = configuration.name

        override fun formElement(
            initialValues: Map<IdentifierSpec, String?>
        ): FormElement = NameSpec().transform(initialValues)
    },
    Phone {
        override fun collectionMode(
            configuration: PaymentSheet.BillingDetailsCollectionConfiguration
        ): PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode = configuration.phone

        override fun formElement(
            initialValues: Map<IdentifierSpec, String?>
        ): FormElement = PhoneSpec().transform(initialValues)
    },
    Email {
        override fun collectionMode(
            configuration: PaymentSheet.BillingDetailsCollectionConfiguration
        ): PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode = configuration.email

        override fun formElement(
            initialValues: Map<IdentifierSpec, String?>
        ): FormElement = EmailSpec().transform(initialValues)
    };

    abstract fun collectionMode(
        configuration: PaymentSheet.BillingDetailsCollectionConfiguration
    ): PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode

    abstract fun formElement(initialValues: Map<IdentifierSpec, String?>): FormElement

    fun isAllowed(configuration: PaymentSheet.BillingDetailsCollectionConfiguration): Boolean {
        val collectionMode = collectionMode(configuration = configuration)
        return collectionMode != PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never
    }

    fun isRequired(configuration: PaymentSheet.BillingDetailsCollectionConfiguration): Boolean {
        val collectionMode = collectionMode(configuration = configuration)
        return collectionMode == PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
    }
}
