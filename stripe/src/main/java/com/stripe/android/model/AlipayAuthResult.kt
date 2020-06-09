package com.stripe.android.model

import com.stripe.android.StripeIntentResult
import kotlinx.android.parcel.Parcelize

@Parcelize
internal data class AlipayAuthResult(
    @StripeIntentResult.Outcome val outcome: Int
) : StripeModel
