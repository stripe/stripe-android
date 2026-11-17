package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.TestUiDefinitionFactoryArgumentsFactory
import com.stripe.android.lpmfoundations.paymentmethod.isSupported
import com.stripe.android.lpmfoundations.paymentmethod.unmetRequirements
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.stripebalance.StripeBalanceFinancialAccounts
import com.stripe.android.testing.FakeErrorReporter
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
            hasCustomerConfiguration = true,
            customerSessionClientSecret = "cuss_123_secret_456",
            stripeBalanceFinancialAccounts = stripeBalanceTestFinancialAccounts,
        )

        assertThat(StripeBalanceDefinition.isSupported(metadata)).isTrue()
    }

    @Test
    fun `does not require a mandate`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
                .stripeIntent(PaymentMethod.Type.StripeBalance),
            hasCustomerConfiguration = true,
            customerSessionClientSecret = "cuss_123_secret_456",
            stripeBalanceFinancialAccounts = stripeBalanceTestFinancialAccounts,
        )

        assertThat(StripeBalanceDefinition.requiresMandate(metadata)).isFalse()
    }

    @Test
    fun `is not supported without a CustomerSession`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
                .stripeIntent(PaymentMethod.Type.StripeBalance),
            hasCustomerConfiguration = true,
            stripeBalanceFinancialAccounts = stripeBalanceTestFinancialAccounts,
        )

        assertThat(StripeBalanceDefinition.unmetRequirements(metadata))
            .containsExactly(AddPaymentMethodRequirement.CustomerSession)
    }

    @Test
    fun `is not supported with a Checkout Session`() {
        val stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
            .stripeIntent(PaymentMethod.Type.StripeBalance)
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = stripeIntent,
            hasCustomerConfiguration = true,
            customerSessionClientSecret = "cuss_123_secret_456",
            integrationMetadata = LpmBillingDetailsCollectionMode.AutomaticWithTax.integrationMetadata(stripeIntent),
            stripeBalanceFinancialAccounts = stripeBalanceTestFinancialAccounts,
        )

        assertThat(StripeBalanceDefinition.unmetRequirements(metadata))
            .containsExactly(AddPaymentMethodRequirement.UnsupportedForCheckoutSession)
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
    fun `is not supported when the buyer has no financial accounts`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
                .stripeIntent(PaymentMethod.Type.StripeBalance),
            stripeBalanceFinancialAccounts = StripeBalanceFinancialAccounts(accounts = emptyList(), currency = "usd"),
        )

        assertThat(StripeBalanceDefinition.isSupported(metadata)).isFalse()
    }

    @Test
    fun `reports an unexpected error if its form is built without financial accounts`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
                .stripeIntent(PaymentMethod.Type.StripeBalance),
            stripeBalanceFinancialAccounts = null,
        )
        val errorReporter = FakeErrorReporter()
        val arguments = TestUiDefinitionFactoryArgumentsFactory.create()
            .create(metadata = metadata, requiresMandate = false)
            .copy(errorReporter = errorReporter)

        StripeBalanceDefinition.uiDefinitionFactory(metadata).createFormElements(metadata, arguments)

        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            ErrorReporter.UnexpectedErrorEvent.STRIPE_BALANCE_FORM_MISSING_FINANCIAL_ACCOUNTS.eventName
        )
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
            hasCustomerConfiguration = true,
            customerSessionClientSecret = "cuss_123_secret_456",
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
