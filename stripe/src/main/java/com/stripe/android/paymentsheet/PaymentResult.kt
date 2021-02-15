package com.stripe.android.paymentsheet

import android.os.Parcelable
import com.stripe.android.model.PaymentIntent
import kotlinx.parcelize.Parcelize

/**
 * The result a payment sheet operation.
 */
sealed class PaymentResult : Parcelable {

    @Parcelize
    data class Completed(
        val paymentIntent: PaymentIntent
    ) : PaymentResult()

    @Parcelize
    data class Failed(
        val error: Throwable,
        val paymentIntent: PaymentIntent?
    ) : PaymentResult()

    @Parcelize
    data class Canceled(
        val mostRecentError: Throwable?,
        val paymentIntent: PaymentIntent?
    ) : PaymentResult()
}
