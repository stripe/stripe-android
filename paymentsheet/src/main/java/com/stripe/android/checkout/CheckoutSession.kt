package com.stripe.android.checkout

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@Poko
@Parcelize
@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CheckoutSession internal constructor(
    val id: String,
    val currency: String,
    val totalSummary: TotalSummary?,
) : Parcelable {

    @Poko
    @Parcelize
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
    ) : Parcelable

    @Poko
    @Parcelize
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class DiscountAmount internal constructor(
        val amount: Long,
        val displayName: String,
    ) : Parcelable

    @Poko
    @Parcelize
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class TaxAmount internal constructor(
        val amount: Long,
        val inclusive: Boolean,
        val displayName: String,
        val percentage: Double,
    ) : Parcelable

    @Poko
    @Parcelize
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class ShippingRate internal constructor(
        val amount: Long,
        val displayName: String,
        val deliveryEstimate: String?,
    ) : Parcelable
}

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutSessionResponse.asCheckoutSession(): CheckoutSession {
    return CheckoutSession(
        id = id,
        currency = currency,
        totalSummary = totalSummary?.asTotalSummary(),
    )
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
        amount = amount,
        displayName = displayName,
        deliveryEstimate = deliveryEstimate,
    )
}
