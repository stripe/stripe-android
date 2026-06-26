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

        val checkout = CheckoutInstances[checkoutSessionMetadata.instancesKey]
            ?: return emptyList()

        val checkoutSession = checkout.checkoutSession.value
        val items = mutableListOf<GooglePayJsonFactory.DisplayItem>()

        items += checkoutSession.lineItems.map { it.asDisplayItem() }
        items += checkoutSession.totalSummary?.discountAmounts.orEmpty().map { it.asDisplayItem() }

        when (checkoutSession.tax.status) {
            CheckoutSession.Tax.Status.RequiresBillingAddress -> {
                items += pendingTaxDisplayItem()
            }
            else -> {
                items += checkoutSession.totalSummary?.taxAmounts.orEmpty().map { it.asDisplayItem() }
            }
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

    private fun pendingTaxDisplayItem(): GooglePayJsonFactory.DisplayItem {
        return GooglePayJsonFactory.DisplayItem(
            label = PENDING_TAX_LABEL,
            type = GooglePayJsonFactory.DisplayItem.Type.TAX,
            price = 0L,
            status = GooglePayJsonFactory.DisplayItem.Status.Pending,
        )
    }

    private const val PENDING_TAX_LABEL = "Tax (pending)"
}
