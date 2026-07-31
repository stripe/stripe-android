package com.stripe.android.paymentelement.confirmation.gpay

import android.content.res.Resources
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.R
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

internal object GooglePayDisplayItemsFactory {

    data class Presentation(
        val displayItems: List<GooglePayJsonFactory.DisplayItem>,
        val customLabel: String?,
        val totalPriceStatus: GooglePayJsonFactory.TransactionInfo.TotalPriceStatus?,
        val forceBillingAddressCollection: Boolean,
    )

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

    fun createCheckoutPresentation(
        resources: Resources,
        paymentMethodMetadata: PaymentMethodMetadata,
        hasQualifiedDefaultBillingTaxEstimate: Boolean,
    ): Presentation {
        val response = paymentMethodMetadata.checkoutSessionResponse
        val isQualifiedAutomaticTaxBilling = response != null &&
            response.automaticTaxEnabled &&
            response.taxAddressSource == CheckoutSessionResponse.TaxAddressSource.BILLING &&
            response.taxStatus == CheckoutSessionResponse.TaxStatus.READY &&
            hasQualifiedDefaultBillingTaxEstimate
        val exclusiveTaxAmounts = response
            ?.totalSummary
            ?.taxAmounts
            .orEmpty()
            .filterNot { it.inclusive }

        return if (isQualifiedAutomaticTaxBilling && exclusiveTaxAmounts.isNotEmpty()) {
            val exclusiveTaxTotal = exclusiveTaxAmounts.sumOf { it.amount }
            Presentation(
                displayItems = listOf(
                    GooglePayJsonFactory.DisplayItem(
                        label = resources.getString(R.string.stripe_google_pay_cost_excluding_tax),
                        type = GooglePayJsonFactory.DisplayItem.Type.SUBTOTAL,
                        price = response.amount - exclusiveTaxTotal,
                    ),
                    GooglePayJsonFactory.DisplayItem(
                        label = resources.getString(R.string.stripe_google_pay_estimated_tax),
                        type = GooglePayJsonFactory.DisplayItem.Type.TAX,
                        price = exclusiveTaxTotal,
                    ),
                ),
                customLabel = resources.getString(R.string.stripe_google_pay_estimated_total),
                totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
                forceBillingAddressCollection = true,
            )
        } else {
            Presentation(
                displayItems = create(paymentMethodMetadata),
                customLabel = null,
                totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
                forceBillingAddressCollection = isQualifiedAutomaticTaxBilling,
            )
        }
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
