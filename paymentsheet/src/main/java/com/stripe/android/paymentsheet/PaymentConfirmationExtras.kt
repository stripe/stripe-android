package com.stripe.android.paymentsheet

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

internal sealed interface PaymentConfirmationExtras : Parcelable {
    @Parcelize
    data class Intent(
        val deferredIntentConfirmationType: DeferredIntentConfirmationType
    ) : PaymentConfirmationExtras
}
