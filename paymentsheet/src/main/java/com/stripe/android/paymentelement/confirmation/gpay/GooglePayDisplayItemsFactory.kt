package com.stripe.android.paymentelement.confirmation.gpay

import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

internal object GooglePayDisplayItemsFactory {

    fun create(paymentMethodMetadata: PaymentMethodMetadata): List<GooglePayJsonFactory.DisplayItem> {
        val response = paymentMethodMetadata.checkoutSessionResponse ?: return emptyList()

        val items = mutableListOf<GooglePayJsonFactory.DisplayItem>()

        items += response.lineItems.map { it.asDisplayItem() }

        response.totalSummary?.let { summary ->
            items += summary.discountAmounts.map { it.asDisplayItem() }
            items += summary.taxAmounts.map { it.asDisplayItem() }
        }

        return items
    }

    private fun CheckoutSessionResponse.LineItem.asDisplayItem(): GooglePayJsonFactory.DisplayItem {
        val label = if (quantity > 1) "$name x$quantity" else name
        return GooglePayJsonFactory.DisplayItem(
            label = label,
            type = GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
            price = unitAmount ?: total,
        )
    }

    private fun CheckoutSessionResponse.DiscountAmount.asDisplayItem(): GooglePayJsonFactory.DisplayItem {
        return GooglePayJsonFactory.DisplayItem(
            label = displayName,
            type = GooglePayJsonFactory.DisplayItem.Type.DISCOUNT,
            price = -amount,
        )
    }

    private fun CheckoutSessionResponse.TaxAmount.asDisplayItem(): GooglePayJsonFactory.DisplayItem {
        return GooglePayJsonFactory.DisplayItem(
            label = displayName,
            type = GooglePayJsonFactory.DisplayItem.Type.TAX,
            price = amount,
        )
    }
}
