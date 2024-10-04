package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlin.math.roundToInt

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class LinkConsumerIncentive(
    val campaign: String,
    val paymentMethod: String,
    val incentiveParams: IncentiveParams,
) : StripeModel

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface IncentiveParams : Parcelable {
    val currency: String
    val formatted: String
    val disclaimer: String

    @Parcelize
    data class Absolute(
        val amount: Long,
        override val currency: String,
    ) : IncentiveParams {

        override val formatted: String
            get() = "Get $${amount / 100}"

        override val disclaimer: String
            get() = "Get \$${amount / 100} back when you pay with your bank. <a href=\"https://link.com/promotion-terms\">See terms.</a>"
    }

    @Parcelize
    data class Relative(
        val percentage: Float,
        override val currency: String,
    ) : IncentiveParams {

        override val formatted: String
            get() = "${(percentage * 100).roundToInt()}% off"

        override val disclaimer: String
            get() = "Get ${(percentage * 100).roundToInt()}% off when you pay with your bank. <a href=\"https://link.com/promotion-terms\">See terms.</a>"
    }
}
