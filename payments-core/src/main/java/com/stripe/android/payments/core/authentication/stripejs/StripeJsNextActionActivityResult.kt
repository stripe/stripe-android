package com.stripe.android.payments.core.authentication.stripejs

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

internal sealed interface StripeJsNextActionActivityResult : Parcelable {
    @Parcelize
    data class Completed(val clientSecret: String) : StripeJsNextActionActivityResult

    @Parcelize
    data object Canceled : StripeJsNextActionActivityResult

    @Parcelize
    data class Failed(val error: Throwable) : StripeJsNextActionActivityResult
}
