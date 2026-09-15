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

        items += response.checkoutItems.flatMap { it.oneTimePrice.items }.map { it.asDisplayItem(context) }
        items += response.subtotalDisplayItem(context)
        items += response.recurringDetails?.totalDiscountAmounts.orEmpty().map { it.asDisplayItem(context) }
        items += response.recurringDetails?.totalTaxAmounts.orEmpty().map { it.asDisplayItem(context) }
        items += response.estimatedTotalLineItem(context)

        return items
    }

    private fun CheckoutSessionResponse.subtotalDisplayItem(
        context: Context,
    ): GooglePayJsonFactory.DisplayItem {
        return GooglePayJsonFactory.DisplayItem(
            label = R.string.stripe_google_pay_cost_excluding_tax.resolvableString.resolve(context),
            type = GooglePayJsonFactory.DisplayItem.Type.SUBTOTAL,
            price = checkoutItems.sumOf { group -> group.oneTimePrice.items.sumOf { it.subtotal } },
        )
    }

    private fun CheckoutSessionResponse.estimatedTotalLineItem(
        context: Context,
    ): GooglePayJsonFactory.DisplayItem {
        return GooglePayJsonFactory.DisplayItem(
            label = R.string.stripe_google_pay_estimated_total.resolvableString.resolve(context),
            type = GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
            price = amount,
        )
    }

    private fun CheckoutSessionResponse.OneTimePriceItem.asDisplayItem(
        context: Context,
    ): GooglePayJsonFactory.DisplayItem {
        val name = price.product.name
        val label = if (quantity > 1) "$name x$quantity" else name
        return GooglePayJsonFactory.DisplayItem(
            label = label.resolvableString.resolve(context),
            type = GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM,
            price = subtotal,
        )
    }

    private fun CheckoutSessionResponse.DiscountAmount.asDisplayItem(
        context: Context,
    ): GooglePayJsonFactory.DisplayItem {
        return GooglePayJsonFactory.DisplayItem(
            label = (displayName ?: coupon.name ?: coupon.code).resolvableString.resolve(context),
            type = GooglePayJsonFactory.DisplayItem.Type.DISCOUNT,
            price = -amount,
        )
    }

    private fun CheckoutSessionResponse.TaxAmount.asDisplayItem(
        context: Context,
    ): GooglePayJsonFactory.DisplayItem {
        return GooglePayJsonFactory.DisplayItem(
            label = taxRate.displayName.resolvableString.resolve(context),
            type = GooglePayJsonFactory.DisplayItem.Type.TAX,
            price = amount,
        )
    }
}
