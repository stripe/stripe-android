package com.stripe.android.model

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
