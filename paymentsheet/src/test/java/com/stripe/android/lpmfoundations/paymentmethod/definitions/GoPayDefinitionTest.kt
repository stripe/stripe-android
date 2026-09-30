package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.lpmfoundations.paymentmethod.isSupported
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
internal class GoPayDefinitionTest {
    @Test
    fun `supports PaymentIntents without native authorization or mandate UI`(
        @TestParameter hasSetupFutureUsage: Boolean,
    ) {
        val intentScenario = if (hasSetupFutureUsage) {
            LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage
        } else {
            LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
        }
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.GoPay),
        )

        assertThat(GoPayDefinition.isSupported(metadata)).isTrue()
        assertThat(GoPayDefinition.formElements(metadata)).isEmpty()
        assertThat(GoPayDefinition.requiresMandate(metadata)).isEqualTo(hasSetupFutureUsage)
    }

    @Test
    fun `does not support SetupIntents`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent
                .stripeIntent(PaymentMethod.Type.GoPay),
        )

        assertThat(GoPayDefinition.isSupported(metadata)).isFalse()
    }

    @Test
    fun `requirements exclude SetupIntents`() {
        assertThat(GoPayDefinition.requirementsToBeUsedAsNewPaymentMethod(hasIntentToSetup = false))
            .containsExactly(AddPaymentMethodRequirement.UnsupportedForSetupIntent)
    }

    @Test
    fun `collects configured contact information`() {
        val formElements = GoPayDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("gopay")),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                ),
            )
        )

        assertThat(formElements).hasSize(2)
        checkPhoneField(formElements, 0)
        checkEmailField(formElements, 1)
    }

    @Test
    fun `collects configured billing address`() {
        val formElements = GoPayDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("gopay")),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                ),
            )
        )

        assertThat(formElements).hasSize(1)
        checkBillingField(formElements, 0)
    }

    @Test
    fun `does not redisplay saved payment methods`() {
        assertThat(GoPayDefinition.supportedAsSavedPaymentMethod).isFalse()
    }
}
