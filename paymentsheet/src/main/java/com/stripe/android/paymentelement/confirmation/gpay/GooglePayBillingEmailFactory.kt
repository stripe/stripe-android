package com.stripe.android.paymentelement.confirmation.gpay

import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.CheckoutSessionPreview

@OptIn(CheckoutSessionPreview::class)
internal object GooglePayBillingEmailFactory {

    fun create(paymentMethodMetadata: PaymentMethodMetadata): String? {
        val checkoutSessionMetadata = paymentMethodMetadata.integrationMetadata
            as? IntegrationMetadata.CheckoutSession ?: return null

        val checkout = CheckoutInstances[checkoutSessionMetadata.instancesKey]
            ?: return null

        return checkout.checkoutSession.value.customerEmail
    }
}
