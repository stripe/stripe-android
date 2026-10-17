package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

class MultibancoDefinitionTest {
    private val multibancoMetadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("multibanco"),
        )
    )

    @Test
    fun `createFormElements returns email field by default`() {
        val formElements = MultibancoDefinition.formElements(multibancoMetadata)

        assertThat(formElements).hasSize(1)
        checkEmailField(formElements, 0)
    }

    @Test
    fun `createFormElements returns all billing details fields when configured`() {
        val formElements = MultibancoDefinition.formElements(
            metadata = multibancoMetadata.copy(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                )
            )
        )

        assertThat(formElements).hasSize(4)
        checkNameField(formElements, 0)
        checkPhoneField(formElements, 1)
        checkEmailField(formElements, 2)
        checkBillingField(formElements, 3)
    }

    @Test
    fun `createFormElements returns contact information fields when configured`() {
        val formElements = MultibancoDefinition.formElements(
            metadata = multibancoMetadata.copy(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                )
            )
        )

        assertThat(formElements).hasSize(2)
        checkPhoneField(formElements, 0)
        checkEmailField(formElements, 1)
    }

    @Test
    fun `createFormElements omits email when set to never`() {
        val formElements = MultibancoDefinition.formElements(
            metadata = multibancoMetadata.copy(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                )
            )
        )

        assertThat(formElements).isEmpty()
    }
}
