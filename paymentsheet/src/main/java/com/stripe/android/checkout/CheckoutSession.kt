package com.stripe.android.checkout

import androidx.annotation.RestrictTo
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.verticalmode.CurrencySelectorOptions
import com.stripe.android.paymentsheet.verticalmode.CurrencySelectorOptionsFactory
import dev.drewhamilton.poko.Poko

@Poko
@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CheckoutSession internal constructor(
    val id: String,
    val status: Status,
    val liveMode: Boolean,
    val currency: String,
    val customerEmail: String?,
    val totalSummary: TotalSummary?,
    val lineItems: List<LineItem>,
    val shippingOptions: List<ShippingRate>,
    internal val currencySelectorOptions: CurrencySelectorOptions?,
) {

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class Status {
        /**
         * The checkout session is still in progress. Payment processing has not started.
         */
        Open,

        /**
         * The checkout session is complete. Payment processing may still be in progress.
         */
        Complete,

        /**
         * The checkout session has expired. No further processing will occur.
         */
        Expired,

        /**
         * A status not recognized by this version of the SDK.
         */
        Unknown,
    }

    @Poko
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class TotalSummary internal constructor(
        val subtotal: Long,
        val totalDueToday: Long,
        val totalAmountDue: Long,
        val discountAmounts: List<DiscountAmount>,
        val taxAmounts: List<TaxAmount>,
        val shippingRate: ShippingRate?,
        val appliedBalance: Long?,
    )

    @Poko
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class DiscountAmount internal constructor(
        val amount: Long,
        val displayName: String,
    )

    @Poko
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class TaxAmount internal constructor(
        val amount: Long,
        val inclusive: Boolean,
        val displayName: String,
        val percentage: Double,
    )

    @Poko
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class ShippingRate internal constructor(
        val id: String,
        val amount: Long,
        val displayName: String,
        val deliveryEstimate: String?,
    )

    @Poko
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class LineItem internal constructor(
        val id: String,
        val name: String,
        val quantity: Int,
        val unitAmount: Long?,
        val subtotal: Long,
        val total: Long,
    )
}

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutSessionResponse.asCheckoutSession(): CheckoutSession {
    return CheckoutSession(
        id = id,
        status = status.asStatus(),
        liveMode = liveMode,
        currency = currency,
        customerEmail = customerEmail,
        totalSummary = totalSummary?.asTotalSummary(),
        lineItems = lineItems.map { it.asLineItem() },
        shippingOptions = shippingOptions.map { it.asShippingRate() },
        currencySelectorOptions = CurrencySelectorOptionsFactory.create(adaptivePricingInfo = adaptivePricingInfo)
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.Status.asStatus(): CheckoutSession.Status {
    return when (this) {
        CheckoutSessionResponse.Status.OPEN -> CheckoutSession.Status.Open
        CheckoutSessionResponse.Status.COMPLETE -> CheckoutSession.Status.Complete
        CheckoutSessionResponse.Status.EXPIRED -> CheckoutSession.Status.Expired
        CheckoutSessionResponse.Status.UNKNOWN -> CheckoutSession.Status.Unknown
    }
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.TotalSummaryResponse.asTotalSummary(): CheckoutSession.TotalSummary {
    return CheckoutSession.TotalSummary(
        subtotal = subtotal,
        totalDueToday = totalDueToday,
        totalAmountDue = totalAmountDue,
        discountAmounts = discountAmounts.map { it.asDiscountAmount() },
        taxAmounts = taxAmounts.map { it.asTaxAmount() },
        shippingRate = shippingRate?.asShippingRate(),
        appliedBalance = appliedBalance,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.DiscountAmount.asDiscountAmount(): CheckoutSession.DiscountAmount {
    return CheckoutSession.DiscountAmount(
        amount = amount,
        displayName = displayName,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.TaxAmount.asTaxAmount(): CheckoutSession.TaxAmount {
    return CheckoutSession.TaxAmount(
        amount = amount,
        inclusive = inclusive,
        displayName = displayName,
        percentage = percentage,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.ShippingRate.asShippingRate(): CheckoutSession.ShippingRate {
    return CheckoutSession.ShippingRate(
        id = id,
        amount = amount,
        displayName = displayName,
        deliveryEstimate = deliveryEstimate,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.LineItem.asLineItem(): CheckoutSession.LineItem {
    return CheckoutSession.LineItem(
        id = id,
        name = name,
        quantity = quantity,
        unitAmount = unitAmount,
        subtotal = subtotal,
        total = total,
    )
}
