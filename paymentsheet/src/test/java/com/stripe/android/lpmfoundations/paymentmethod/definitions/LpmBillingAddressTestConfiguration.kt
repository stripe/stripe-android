package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.SetupIntentFactory

internal data class LpmBillingAddressTestConfiguration(
    val paymentMethodType: PaymentMethod.Type,
    val billingDetailsCollectionMode: LpmBillingDetailsCollectionMode,
    val intentScenario: IntentScenario,
    val termsDisplay: PaymentSheet.TermsDisplay,
) {
    fun metadata(): PaymentMethodMetadata {
        return PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(paymentMethodType),
            billingDetailsCollectionConfiguration = billingDetailsCollectionMode
                .billingDetailsCollectionConfiguration(),
            termsDisplay = mapOf(paymentMethodType to termsDisplay),
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
            AutomaticWithoutTax -> PaymentSheet.BillingDetailsCollectionConfiguration()
            Full -> PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            )
        }
    }
}
