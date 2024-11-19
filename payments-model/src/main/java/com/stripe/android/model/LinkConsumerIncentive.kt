package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class LinkConsumerIncentive(
    val incentiveParams: IncentiveParams,
    val incentiveDisplayText: String?,
) : StripeModel {

    @Parcelize
    data class IncentiveParams(
        val paymentMethod: String,
    ) : StripeModel
}
