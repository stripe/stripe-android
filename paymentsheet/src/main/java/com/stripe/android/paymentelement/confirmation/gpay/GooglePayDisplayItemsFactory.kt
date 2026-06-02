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

        val checkoutSession = checkout.checkoutSession.value
        val items = mutableListOf<GooglePayJsonFactory.DisplayItem>()

        items += checkoutSession.lineItems.map { it.asDisplayItem() }

        checkoutSession.totalSummary?.let { summary ->
            items += summary.discountAmounts.map { it.asDisplayItem() }
            items += summary.taxAmounts.map { it.asDisplayItem() }
        }

        return items
    }

    private fun CheckoutSession.LineItem.asDisplayItem(): GooglePayJsonFactory.DisplayItem {
        val label = if (quantity > 1) "$name x$quantity" else name
        return GooglePayJsonFactory.DisplayItem(
            label = label,
            type = GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
            price = unitAmount ?: total,
        )
    }

    private fun CheckoutSession.DiscountAmount.asDisplayItem(): GooglePayJsonFactory.DisplayItem {
        return GooglePayJsonFactory.DisplayItem(
            label = displayName,
            type = GooglePayJsonFactory.DisplayItem.Type.DISCOUNT,
            price = -amount,
        )
    }

    private fun CheckoutSession.TaxAmount.asDisplayItem(): GooglePayJsonFactory.DisplayItem {
        return GooglePayJsonFactory.DisplayItem(
            label = displayName,
            type = GooglePayJsonFactory.DisplayItem.Type.TAX,
            price = amount,
        )
    }
}
