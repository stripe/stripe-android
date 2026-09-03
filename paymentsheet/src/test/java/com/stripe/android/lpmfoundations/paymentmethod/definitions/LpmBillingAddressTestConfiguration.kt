package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.SetupIntentFactory

internal data class LpmBillingAddressTestConfiguration(
    val paymentMethodType: PaymentMethod.Type,
    val billingDetailsCollectionMode: LpmBillingDetailsCollectionMode,
    val intentScenario: IntentScenario,
    val termsDisplay: PaymentSheet.TermsDisplay,
) {
    fun metadata(): PaymentMethodMetadata {
        val stripeIntent = intentScenario.stripeIntent(paymentMethodType)
        return PaymentMethodMetadataFactory.create(
            stripeIntent = stripeIntent,
            billingDetailsCollectionConfiguration = billingDetailsCollectionMode
                .billingDetailsCollectionConfiguration(),
            termsDisplay = mapOf(paymentMethodType to termsDisplay),
            integrationMetadata = billingDetailsCollectionMode.integrationMetadata(stripeIntent),
        )
    }

    override fun toString(): String {
        return "${paymentMethodType.code}-${billingDetailsCollectionMode.name}-" +
            "${intentScenario.name}-${termsDisplay.name}"
    }

    internal enum class IntentScenario {
        PaymentIntent,
        PaymentIntentWithSetupFutureUsage,
        SetupIntent,
        ;

        fun stripeIntent(paymentMethodType: PaymentMethod.Type): StripeIntent {
            return when (this) {
                PaymentIntent -> PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf(paymentMethodType.code),
                )
                PaymentIntentWithSetupFutureUsage -> PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf(paymentMethodType.code),
                    setupFutureUsage = StripeIntent.Usage.OffSession,
                )
                SetupIntent -> SetupIntentFactory.create(
                    paymentMethodTypes = listOf(paymentMethodType.code),
                )
            }
        }
    }
}

internal enum class LpmBillingDetailsCollectionMode {
    Never,
    AutomaticWithoutTax,
    AutomaticWithTax,
    Full,
    ;

    fun billingDetailsCollectionConfiguration(): PaymentSheet.BillingDetailsCollectionConfiguration {
        return when (this) {
            Never -> PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            )
            AutomaticWithoutTax,
            AutomaticWithTax,
            -> PaymentSheet.BillingDetailsCollectionConfiguration()
            Full -> PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            )
        }
    }

    fun integrationMetadata(stripeIntent: StripeIntent): IntegrationMetadata {
        return if (this == AutomaticWithTax) {
            val checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                automaticTaxEnabled = true,
                taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
            )
            IntegrationMetadata.CheckoutSession(
                id = checkoutSessionResponse.id,
                instancesKey = "key",
                checkoutSessionResponse = checkoutSessionResponse,
            )
        } else {
            PaymentMethodMetadataFactory.defaultIntegrationMetadata(stripeIntent)
        }
    }
}
