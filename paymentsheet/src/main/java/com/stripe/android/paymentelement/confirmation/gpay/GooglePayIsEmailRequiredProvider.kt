package com.stripe.android.paymentelement.confirmation.gpay

import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.checkout.CheckoutSession
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet

@OptIn(CheckoutSessionPreview::class)
internal object GooglePayIsEmailRequiredProvider {

    fun get(paymentMethodMetadata: PaymentMethodMetadata, configuration: CommonConfiguration): Boolean {
        val checkoutSessionMetadata = paymentMethodMetadata.integrationMetadata
            as? IntegrationMetadata.CheckoutSession ?: return false

        val checkout = CheckoutInstances[checkoutSessionMetadata.instancesKey]
            ?: return false

        val checkoutSession = checkout.checkoutSession.value

        return collectEmailForCheckoutSession(checkoutSession, configuration.billingDetailsCollectionConfiguration) ||
            configuration.billingDetailsCollectionConfiguration.collectsEmail
    }

    private fun collectEmailForCheckoutSession(
        checkoutSession: CheckoutSession,
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
    ) = billingDetailsCollectionConfiguration.email !=
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never &&
        checkoutSession.customerEmail == null
}
