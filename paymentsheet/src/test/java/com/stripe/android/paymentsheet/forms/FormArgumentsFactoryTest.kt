package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.SupportedPaymentMethodFixtures
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.uicore.elements.FormFieldId
import org.junit.Test

class FormArgumentsFactoryTest {
    @Test
    fun `Checkout email prefills forms without becoming a card billing default`() {
        val metadata = PaymentMethodMetadataFactory.create(
            integrationMetadata = IntegrationMetadata.CheckoutSession(
                id = "cs_test",
                instancesKey = "test",
                checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
                collectedEmail = "checkout@example.com",
            ),
            defaultBillingDetails = PaymentSheet.BillingDetails(),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                attachDefaultsToPaymentMethod = true,
            ),
        )
        val arguments = FormArgumentsFactory.create(SupportedPaymentMethodFixtures.card.code, metadata)
        assertThat(arguments.prefillEmail).isEqualTo("checkout@example.com")
        assertThat(arguments.billingDetails?.email).isNull()
        assertThat(arguments.noUserInteractionFormFieldValues().fieldValuePairs)
            .doesNotContainKey(FormFieldId.Email)
    }

    @Test
    fun `Create correct FormArguments with custom billing details collection`() {
        val actualFromArguments = testCardFormArguments(
            config = PaymentSheetFixtures.CONFIG_BILLING_DETAILS_COLLECTION,
        )

        assertThat(actualFromArguments.billingDetailsCollectionConfiguration).isEqualTo(
            PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                attachDefaultsToPaymentMethod = true,
            )
        )
    }

    private fun testCardFormArguments(
        config: PaymentSheet.Configuration = PaymentSheetFixtures.CONFIG_MINIMUM,
    ): FormArguments {
        return FormArgumentsFactory.create(
            paymentMethodCode = SupportedPaymentMethodFixtures.card.code,
            metadata = PaymentMethodMetadataFactory.create(
                billingDetailsCollectionConfiguration = config.billingDetailsCollectionConfiguration,
            ),
        )
    }
}
