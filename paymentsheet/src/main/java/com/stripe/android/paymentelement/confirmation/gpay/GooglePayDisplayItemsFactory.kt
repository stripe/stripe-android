package com.stripe.android.paymentelement.confirmation.gpay

import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.R
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

internal object GooglePayDisplayItemsFactory {

    fun create(paymentMethodMetadata: PaymentMethodMetadata): List<GooglePayDisplayItem> {
        val response = (paymentMethodMetadata.integrationMetadata as? IntegrationMetadata.CheckoutSession)
            ?.checkoutSessionResponse ?: return emptyList()

        val items = mutableListOf<GooglePayDisplayItem>()

        items += response.lineItems.map { it.asDisplayItem() }

        response.totalSummary?.let { summary ->
            items += summary.subtotalDisplayItem()
            items += summary.discountAmounts.map { it.asDisplayItem() }
            items += summary.taxAmounts.map { it.asDisplayItem() }
            items += summary.estimatedTotalLineItem()
        }

        return items
    }

    private fun CheckoutSessionResponse.TotalSummaryResponse.subtotalDisplayItem(): GooglePayDisplayItem {
        return GooglePayDisplayItem(
            label = R.string.stripe_google_pay_cost_excluding_tax.resolvableString,
            type = GooglePayJsonFactory.DisplayItem.Type.SUBTOTAL,
            price = subtotal,
        )
    }

    private fun CheckoutSessionResponse.TotalSummaryResponse.estimatedTotalLineItem(): GooglePayDisplayItem {
        return GooglePayDisplayItem(
            label = R.string.stripe_google_pay_estimated_total.resolvableString,
            type = GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
            price = totalAmountDue,
        )
    }

    private fun CheckoutSessionResponse.LineItem.asDisplayItem(): GooglePayDisplayItem {
        val label = if (quantity > 1) "$name x$quantity" else name
        return GooglePayDisplayItem(
            label = label.resolvableString,
            type = GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
            price = unitAmount ?: total,
        )
    }

    private fun CheckoutSessionResponse.DiscountAmount.asDisplayItem(): GooglePayDisplayItem {
        return GooglePayDisplayItem(
            label = displayName.resolvableString,
            type = GooglePayJsonFactory.DisplayItem.Type.DISCOUNT,
            price = -amount,
        )
    }

    private fun CheckoutSessionResponse.TaxAmount.asDisplayItem(): GooglePayDisplayItem {
        return GooglePayDisplayItem(
            label = displayName.resolvableString,
            type = GooglePayJsonFactory.DisplayItem.Type.TAX,
            price = amount,
        )
    }
}
