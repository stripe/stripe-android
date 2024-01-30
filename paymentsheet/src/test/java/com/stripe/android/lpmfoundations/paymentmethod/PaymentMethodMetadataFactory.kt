package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet

internal object PaymentMethodMetadataFactory {
    fun create(
        stripeIntent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        merchantName: String = "Test Merchant",
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
            PaymentSheet.BillingDetailsCollectionConfiguration(),
        defaultBillingDetails: PaymentSheet.BillingDetails? = null,
        preferredNetworks: List<CardBrand> = emptyList(),
        allowsDelayedPaymentMethods: Boolean = true,
        financialConnectionsAvailable: Boolean = true,
    ): PaymentMethodMetadata {
        return PaymentMethodMetadata(
            stripeIntent = stripeIntent,
            merchantName = merchantName,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
            defaultBillingDetails = defaultBillingDetails,
            preferredNetworks = preferredNetworks,
            allowsDelayedPaymentMethods = allowsDelayedPaymentMethods,
            financialConnectionsAvailable = financialConnectionsAvailable,
        )
    }
}
