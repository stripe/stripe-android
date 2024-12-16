package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
@Serializable
data class LinkConsumerIncentive(
    val incentiveParams: IncentiveParams,
    val incentiveDisplayText: String?,
) : StripeModel {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    @Serializable
    data class IncentiveParams(
        val paymentMethod: String,
    ) : StripeModel
}
