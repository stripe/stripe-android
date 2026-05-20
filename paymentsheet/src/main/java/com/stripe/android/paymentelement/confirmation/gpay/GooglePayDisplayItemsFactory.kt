package com.stripe.android.paymentelement.confirmation.gpay

import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.checkout.CheckoutSession
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.CheckoutSessionPreview

@OptIn(CheckoutSessionPreview::class)
internal object GooglePayDisplayItemsFactory {

    fun create(paymentMethodMetadata: PaymentMethodMetadata): List<GooglePayJsonFactory.DisplayItem> {
        val checkoutSessionMetadata = paymentMethodMetadata.integrationMetadata
            as? IntegrationMetadata.CheckoutSession ?: return emptyList()

        val checkout = CheckoutInstances[checkoutSessionMetadata.instancesKey].firstOrNull()
            ?: return emptyList()

        return checkout.checkoutSession.value.lineItems.map { it.asDisplayItem() }
    }

    private fun CheckoutSession.LineItem.asDisplayItem(): GooglePayJsonFactory.DisplayItem {
        return GooglePayJsonFactory.DisplayItem(
            label = name,
            type = GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
            price = total,
        )
    }
}
