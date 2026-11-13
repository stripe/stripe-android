package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.TestUiDefinitionFactoryArgumentsFactory
import com.stripe.android.lpmfoundations.paymentmethod.isSupported
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormFieldId
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
internal class StripeBalanceDefinitionTest {
    @Test
    fun `supports the documented intent modes`(
        @TestParameter intentScenario: LpmBillingAddressTestConfiguration.IntentScenario,
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.StripeBalance),
            stripeBalanceFinancialAccounts = stripeBalanceTestFinancialAccounts,
        )

        assertThat(StripeBalanceDefinition.isSupported(metadata)).isTrue()
        assertThat(StripeBalanceDefinition.requiresMandate(metadata)).isFalse()
    }

    @Test
    fun `is not supported when the buyer's financial accounts didn't load`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
                .stripeIntent(PaymentMethod.Type.StripeBalance),
            stripeBalanceFinancialAccounts = null,
        )

        assertThat(StripeBalanceDefinition.isSupported(metadata)).isFalse()
    }

    @Test
    fun `shows the account picker before contact information and the billing address`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
                .stripeIntent(PaymentMethod.Type.StripeBalance),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            ),
            stripeBalanceFinancialAccounts = stripeBalanceTestFinancialAccounts,
        )

        val formElements = requireNotNull(
            metadata.formElementsForCode(
                code = PaymentMethod.Type.StripeBalance.code,
                uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(),
            )
        )

        assertThat(formElements.map { it.identifier }).containsExactly(
            FormFieldId.Generic("stripe_balance_financial_accounts"),
            FormFieldId.Generic("billing_details[email]_section"),
            FormFieldId.Generic("billing_details[address]_section"),
        ).inOrder()
    }

    @Test
    fun `does not redisplay saved payment methods`() {
        assertThat(StripeBalanceDefinition.supportedAsSavedPaymentMethod).isFalse()
    }
}
