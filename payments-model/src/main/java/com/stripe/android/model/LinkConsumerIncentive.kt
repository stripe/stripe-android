package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class LinkConsumerIncentive(
    val campaign: String,
    val incentiveParams: IncentiveParams,
) : StripeModel {

    @Parcelize
    data class IncentiveParams(
        val amountFlat: Long?,
        val amountPercent: Float?,
        val currency: String?,
        val paymentMethod: String,
    ) : StripeModel
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun LinkConsumerIncentive.toPaymentMethodIncentive(): PaymentMethodIncentive? {
    return if (incentiveParams.amountFlat != null && incentiveParams.currency != null) {
        PaymentMethodIncentive(
            identifier = incentiveParams.paymentMethod,
            displayText = "$5", // TODO
        )
    } else if (incentiveParams.amountPercent != null) {
        PaymentMethodIncentive(
            identifier = incentiveParams.paymentMethod,
            displayText = "5%", // TODO
        )
    } else {
        null
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class PaymentMethodIncentive(
    val identifier: String,
    val displayText: String,
) : Parcelable
