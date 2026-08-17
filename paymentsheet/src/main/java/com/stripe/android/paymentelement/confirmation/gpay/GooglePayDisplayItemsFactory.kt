package com.stripe.android.paymentelement.confirmation.gpay

import android.content.Context
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.R
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

internal object GooglePayDisplayItemsFactory {

    fun create(
        paymentMethodMetadata: PaymentMethodMetadata,
        context: Context,
    ): List<GooglePayJsonFactory.DisplayItem> {
        val response = (paymentMethodMetadata.integrationMetadata as? IntegrationMetadata.CheckoutSession)
            ?.checkoutSessionResponse ?: return emptyList()

        return create(response, context)
    }

    fun create(
        response: CheckoutSessionResponse,
        context: Context,
    ): List<GooglePayJsonFactory.DisplayItem> {
        val items = mutableListOf<GooglePayJsonFactory.DisplayItem>()

        items += response.lineItems.map { it.asDisplayItem(context) }

        response.totalSummary?.let { summary ->
            items += summary.subtotalDisplayItem(context)
            items += summary.discountAmounts.map { it.asDisplayItem(context) }
            items += summary.taxAmounts.map { it.asDisplayItem(context) }
            items += summary.estimatedTotalLineItem(context)
        }

        return items
    }

    private fun CheckoutSessionResponse.TotalSummaryResponse.subtotalDisplayItem(
        context: Context,
    ): GooglePayJsonFactory.DisplayItem {
        return GooglePayJsonFactory.DisplayItem(
            label = R.string.stripe_google_pay_cost_excluding_tax.resolvableString.resolve(context),
            type = GooglePayJsonFactory.DisplayItem.Type.SUBTOTAL,
            price = subtotal,
        )
    }

    private fun CheckoutSessionResponse.TotalSummaryResponse.estimatedTotalLineItem(
        context: Context,
    ): GooglePayJsonFactory.DisplayItem {
        return GooglePayJsonFactory.DisplayItem(
            label = R.string.stripe_google_pay_estimated_total.resolvableString.resolve(context),
            type = GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
            price = totalAmountDue,
        )
    }

    private fun CheckoutSessionResponse.LineItem.asDisplayItem(
        context: Context,
    ): GooglePayJsonFactory.DisplayItem {
        val label = if (quantity > 1) "$name x$quantity" else name
        return GooglePayJsonFactory.DisplayItem(
            label = label.resolvableString.resolve(context),
            type = GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
            price = unitAmount ?: total,
        )
    }

    private fun CheckoutSessionResponse.DiscountAmount.asDisplayItem(
        context: Context,
    ): GooglePayJsonFactory.DisplayItem {
        return GooglePayJsonFactory.DisplayItem(
            label = displayName.resolvableString.resolve(context),
            type = GooglePayJsonFactory.DisplayItem.Type.DISCOUNT,
            price = -amount,
        )
    }

    private fun CheckoutSessionResponse.TaxAmount.asDisplayItem(
        context: Context,
    ): GooglePayJsonFactory.DisplayItem {
        return GooglePayJsonFactory.DisplayItem(
            label = displayName.resolvableString.resolve(context),
            type = GooglePayJsonFactory.DisplayItem.Type.TAX,
            price = amount,
        )
    }
}
