package com.stripe.android.paymentsheet.forms

import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.BillingDetailsCollectionConfiguration
import com.stripe.android.lpmfoundations.luxe.LpmRepositoryTestHelpers
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import org.junit.Test

class FormArgumentsFactoryTest {
    @Test
    fun `Create correct FormArguments with custom billing details collection`() {
        val actualFromArguments = testCardFormArguments(
            config = PaymentSheetFixtures.CONFIG_BILLING_DETAILS_COLLECTION,
        )

        assertThat(actualFromArguments.billingDetailsCollectionConfiguration).isEqualTo(
            BillingDetailsCollectionConfiguration(
                name = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                attachDefaultsToPaymentMethod = true,
            )
        )
    }

    private fun testCardFormArguments(
        config: PaymentSheet.Configuration = PaymentSheetFixtures.CONFIG_MINIMUM,
    ): FormArguments {
        return FormArgumentsFactory.create(
            paymentMethodCode = LpmRepositoryTestHelpers.card.code,
            metadata = PaymentMethodMetadataFactory.create(
                billingDetailsCollectionConfiguration = config.billingDetailsCollectionConfiguration,
            ),
        )
    }
}
