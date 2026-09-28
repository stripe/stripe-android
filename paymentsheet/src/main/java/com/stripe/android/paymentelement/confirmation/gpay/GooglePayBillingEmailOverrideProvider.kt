package com.stripe.android.paymentelement.confirmation.gpay

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

/**
 * Computes the billing email to force onto the Google Pay payment method, scoped to Checkout
 * Sessions. The Checkout Session's customer_email (merged into [CommonConfiguration.defaultBillingDetails]
 * by the checkout configuration merger) is authoritative: the backend rejects confirmation if the
 * payment method email differs from it, so it must override whatever email Google Pay returns.
 *
 * Returns null outside of Checkout Sessions so normal Google Pay flows keep Google Pay's own email.
 */
internal object GooglePayBillingEmailOverrideProvider {

    fun get(
        configuration: CommonConfiguration,
        paymentMethodMetadata: PaymentMethodMetadata,
    ): String? {
        if (paymentMethodMetadata.integrationMetadata !is IntegrationMetadata.CheckoutSession) {
            return null
        }

        return configuration.defaultBillingDetails?.email
    }
}
