package com.stripe.android.paymentelement.confirmation.gpay

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.PaymentSheet

internal object GooglePayIsEmailRequiredProvider {

    fun get(
        configuration: CommonConfiguration,
        paymentMethodMetadata: PaymentMethodMetadata,
    ): Boolean {
        val integrationMetadata = paymentMethodMetadata.integrationMetadata
        if (integrationMetadata !is IntegrationMetadata.CheckoutSession) {
            return configuration.billingDetailsCollectionConfiguration.collectsEmail
        }

        val checkoutSessionIsMissingEmail = integrationMetadata.effectiveEmail == null

        return configuration.billingDetailsCollectionConfiguration.collectsEmail ||
            (
                checkoutSessionIsMissingEmail &&
                    configuration.billingDetailsCollectionConfiguration.email !=
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never
                )
    }
}
