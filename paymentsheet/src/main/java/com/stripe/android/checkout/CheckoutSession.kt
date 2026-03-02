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
        totalSummary = totalSummary?.let { summary ->
            CheckoutSession.TotalSummary(
                subtotal = summary.subtotal,
                totalDueToday = summary.totalDueToday,
                totalAmountDue = summary.totalAmountDue,
                discountAmounts = summary.discountAmounts.map { discount ->
                    CheckoutSession.DiscountAmount(
                        amount = discount.amount,
                        displayName = discount.displayName,
                    )
                },
                taxAmounts = summary.taxAmounts.map { tax ->
                    CheckoutSession.TaxAmount(
                        amount = tax.amount,
                        inclusive = tax.inclusive,
                        displayName = tax.displayName,
                        percentage = tax.percentage,
                    )
                },
                shippingRate = summary.shippingRate?.let { shipping ->
                    CheckoutSession.ShippingRate(
                        amount = shipping.amount,
                        displayName = shipping.displayName,
                        deliveryEstimate = shipping.deliveryEstimate,
                    )
                },
                appliedBalance = summary.appliedBalance,
            )
        },
    )
}
