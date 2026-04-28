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
    val paymentStatus: PaymentStatus,
    val liveMode: Boolean,
    val currency: String,
    val customer: String?,
    val customerEmail: String?,
    val totalSummary: TotalSummary?,
    val discountAmounts: List<DiscountAmount>,
    val lineItems: List<LineItem>,
    val shippingOptions: List<ShippingRate>,
    val billingAddress: Address?,
    val shippingAddress: Address?,
    internal val currencySelectorOptions: CurrencySelectorOptions?,
) {

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class Status {
        OPEN,
        COMPLETE,
        EXPIRED,
        UNKNOWN,
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class PaymentStatus {
        PAID,
        UNPAID,
        NO_PAYMENT_REQUIRED,
        UNKNOWN,
    }

    @Poko
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Address internal constructor(
        val city: String?,
        val country: String?,
        val line1: String?,
        val line2: String?,
        val postalCode: String?,
        val state: String?,
    )

    @Poko
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class TotalSummary internal constructor(
        val subtotal: Long,
        val totalDueToday: Long,
        val totalAmountDue: Long,
        val discountAmounts: List<DiscountAmount>,
        val discountTotal: Long?,
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
internal fun CheckoutSessionResponse.asCheckoutSession(
    billingAddress: Address.State?,
    shippingAddress: Address.State?,
): CheckoutSession {
    return CheckoutSession(
        id = id,
        status = status.asStatus(),
        paymentStatus = paymentStatus.asPaymentStatus(),
        liveMode = livemode,
        currency = currency,
        customer = customer?.id,
        customerEmail = customerEmail,
        totalSummary = totalSummary?.asTotalSummary(),
        discountAmounts = totalSummary?.discountAmounts?.map { it.asDiscountAmount() } ?: emptyList(),
        lineItems = lineItems.map { it.asLineItem() },
        shippingOptions = shippingOptions.map { it.asShippingRate() },
        billingAddress = billingAddress?.asAddress(),
        shippingAddress = shippingAddress?.asAddress(),
        currencySelectorOptions = CurrencySelectorOptionsFactory.create(adaptivePricingInfo = adaptivePricingInfo)
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun String.asStatus(): CheckoutSession.Status {
    return when (this) {
        "open" -> CheckoutSession.Status.OPEN
        "complete" -> CheckoutSession.Status.COMPLETE
        "expired" -> CheckoutSession.Status.EXPIRED
        else -> CheckoutSession.Status.UNKNOWN
    }
}

@OptIn(CheckoutSessionPreview::class)
private fun String.asPaymentStatus(): CheckoutSession.PaymentStatus {
    return when (this) {
        "paid" -> CheckoutSession.PaymentStatus.PAID
        "unpaid" -> CheckoutSession.PaymentStatus.UNPAID
        "no_payment_required" -> CheckoutSession.PaymentStatus.NO_PAYMENT_REQUIRED
        else -> CheckoutSession.PaymentStatus.UNKNOWN
    }
}

@OptIn(CheckoutSessionPreview::class)
private fun Address.State.asAddress(): CheckoutSession.Address {
    return CheckoutSession.Address(
        city = city,
        country = country,
        line1 = line1,
        line2 = line2,
        postalCode = postalCode,
        state = state,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.TotalSummaryResponse.asTotalSummary(): CheckoutSession.TotalSummary {
    return CheckoutSession.TotalSummary(
        subtotal = subtotal,
        totalDueToday = totalDueToday,
        totalAmountDue = totalAmountDue,
        discountAmounts = discountAmounts.map { it.asDiscountAmount() },
        discountTotal = discountTotal,
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
