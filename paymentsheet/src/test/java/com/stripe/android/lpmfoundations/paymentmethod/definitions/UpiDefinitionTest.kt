package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodRegistry
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.lpmfoundations.paymentmethod.isSupported
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Test

internal class UpiDefinitionTest {
    @Test
    fun `UPI is registered and supports one time payments without VPA collection`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("upi")),
        )

        assertThat(PaymentMethodRegistry.definitionsByCode["upi"]).isEqualTo(UpiDefinition)
        assertThat(UpiDefinition.isSupported(metadata)).isTrue()
        assertThat(UpiDefinition.formElements(metadata)).isEmpty()
        assertThat(UpiDefinition.requiresMandate(metadata)).isFalse()
    }

    @Test
    fun `UPI does not support SetupIntents`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent
                .stripeIntent(PaymentMethod.Type.Upi),
        )

        assertThat(UpiDefinition.isSupported(metadata)).isFalse()
    }

    @Test
    fun `UPI does not support setup future usage`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage
                .stripeIntent(PaymentMethod.Type.Upi),
        )

        assertThat(UpiDefinition.isSupported(metadata)).isFalse()
    }

    @Test
    fun `UPI honors configured billing collection`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("upi")),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            ),
        )

        val elements = UpiDefinition.formElements(metadata)
        assertThat(elements).hasSize(2)
        checkPhoneField(elements, 0)
        checkBillingField(elements, 1)
    }

    @Test
    fun `UPI cannot be selected as a saved payment method`() {
        assertThat(UpiDefinition.supportedAsSavedPaymentMethod).isFalse()
    }
}
