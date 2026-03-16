package com.stripe.android.paymentsheet.repositories

internal object TotalSummaryResponseFactory {
    fun create(
        subtotal: Long = 1000L,
        totalDueToday: Long = 1000L,
        totalAmountDue: Long = 1000L,
        discountAmounts: List<CheckoutSessionResponse.DiscountAmount> = emptyList(),
        taxAmounts: List<CheckoutSessionResponse.TaxAmount> = emptyList(),
        shippingRate: CheckoutSessionResponse.ShippingRate? = null,
        appliedBalance: Long? = null,
    ): CheckoutSessionResponse.TotalSummaryResponse {
        return CheckoutSessionResponse.TotalSummaryResponse(
            subtotal = subtotal,
            totalDueToday = totalDueToday,
            totalAmountDue = totalAmountDue,
            discountAmounts = discountAmounts,
            taxAmounts = taxAmounts,
            shippingRate = shippingRate,
            appliedBalance = appliedBalance,
        )
    }
}
