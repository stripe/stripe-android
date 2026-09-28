package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

class PromptPayDefinitionTest {
    private val promptPayMetadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("promptpay"),
        )
    )

    @Test
    fun `createFormElements returns minimal set of fields`() {
        val formElements = PromptPayDefinition.formElements(promptPayMetadata)

        assertThat(formElements).hasSize(1)

        checkEmailField(formElements, 0)
    }

    @Test
    fun `createFormElements returns billing address collection fields`() {
        val formElements = PromptPayDefinition.formElements(
            metadata = promptPayMetadata.copy(
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
        checkEmailField(formElements, 1)
        checkPhoneField(formElements, 2)
        checkBillingField(formElements, 3)
    }

    @Test
    fun `createFormElements omits billing address collection fields when specified as never`() {
        val formElements = PromptPayDefinition.formElements(
            metadata = promptPayMetadata.copy(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                )
            )
        )

        assertThat(formElements).isEmpty()
    }
}
