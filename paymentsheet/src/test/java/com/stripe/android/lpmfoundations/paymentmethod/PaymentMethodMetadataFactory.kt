package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration

internal object PaymentMethodMetadataFactory {
    fun create(
        stripeIntent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
            BillingDetailsCollectionConfiguration(),
        allowsDelayedPaymentMethods: Boolean = true,
        financialConnectionsAvailable: Boolean = true,
    ): PaymentMethodMetadata {
        return PaymentMethodMetadata(
            stripeIntent = stripeIntent,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
            allowsDelayedPaymentMethods = allowsDelayedPaymentMethods,
            financialConnectionsAvailable = financialConnectionsAvailable,
        )
    }
}
