package com.stripe.android.payments.core.authentication.stripejs

import android.os.Parcelable
import com.stripe.android.model.StripeIntent
import kotlinx.parcelize.Parcelize

@Parcelize
data class StripeJsNextActionArgs(
    val publishableKey: String,
    val intent: StripeIntent,
) : Parcelable
